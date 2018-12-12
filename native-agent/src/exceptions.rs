use env::JvmTiEnv;
use errors::*;
use jni::JniEnv;
use std::ffi::CStr;
use std::ptr;
use std::slice;

use jvmti::{
    jclass, jdouble, jfloat, jint, jlocation, jlong, jmethodID, jobject, jobjectArray, jsize,
    jthread, jvmtiError_JVMTI_ERROR_ABSENT_INFORMATION, jvmtiError_JVMTI_ERROR_NATIVE_METHOD,
    jvmtiFrameInfo, jvmtiLocalVariableEntry,
};

pub fn inner_callback(
    mut jvmti_env: JvmTiEnv,
    mut jni_env: JniEnv,
    thread: jthread,
    exception: jobject,
) -> Result<()> {
    trace!("on_exception called");
    let class = jni_env.find_class("com/rollbar/jvmti/ThrowableCache")?;
    let should_cache_method =
        jni_env.get_static_method_id(class, "shouldCacheThrowable", "(Ljava/lang/Throwable;I)Z")?;

    let num_frames = jvmti_env.get_frame_count(thread)?;

    let shouldCache =
        jni_env.call_static_LI_Z_method(class, should_cache_method, exception, num_frames)?;

    if !shouldCache {
        return Ok(());
    }

    let cache_add_method = jni_env.get_static_method_id(
        class,
        "add",
        "(Ljava/lang/Throwable;[Lcom/rollbar/jvmti/CacheFrame;)V",
    )?;

    let start_depth = 0;
    let frames = build_stack_trace_frames(jvmti_env, jni_env, thread, start_depth, num_frames)?;

    jni_env.call_static_LAL_V_method(class, cache_add_method, exception, frames)?;
    trace!("on_exception exit");
    Ok(())
}

fn build_stack_trace_frames(
    mut jvmti_env: JvmTiEnv,
    mut jni_env: JniEnv,
    thread: jthread,
    start_depth: jint,
    num_frames: jint,
) -> Result<jobjectArray> {
    let mut frames: Vec<jvmtiFrameInfo> = Vec::with_capacity(num_frames as usize);
    let mut num_frames_returned: jint = 0;
    jvmti_env.get_stack_trace(
        thread,
        start_depth,
        num_frames,
        frames.as_mut_ptr(),
        &mut num_frames_returned,
    )?;
    if num_frames_returned >= 0 && num_frames_returned as usize > frames.len() {
        debug_assert!(num_frames_returned as usize <= frames.capacity());
        unsafe {
            frames.set_len(num_frames_returned as usize);
        }
    }
    let class = jni_env.find_class("com/rollbar/jvmti/CacheFrame")?;
    let result = jni_env.new_object_array(num_frames_returned, class, ptr::null_mut())?;
    for i in 0..num_frames_returned {
        let frame = build_frame(
            &mut jvmti_env,
            &mut jni_env,
            thread,
            start_depth + i,
            frames[i as usize].method,
            frames[i as usize].location,
        )?;
        jni_env.set_object_array_element(result, i, frame)?;
    }
    Ok(result)
}

fn build_frame(
    jvmti_env: &mut JvmTiEnv,
    jni_env: &mut JniEnv,
    thread: jthread,
    depth: jint,
    method: jmethodID,
    location: jlocation,
) -> Result<jobject> {
    let mut num_entries: jint = 0;
    let mut local_var_table: *mut jvmtiLocalVariableEntry = ptr::null_mut();

    if let Err(e) =
        jvmti_env.get_local_variable_table(method, &mut num_entries, &mut local_var_table)
    {
        match e {
            Error(ErrorKind::JvmTi(_, rc), _)
                if rc == jvmtiError_JVMTI_ERROR_ABSENT_INFORMATION as jint
                    || rc == jvmtiError_JVMTI_ERROR_NATIVE_METHOD as jint =>
            {
                return make_frame_object(jvmti_env, jni_env, method, ptr::null_mut());
            }
            _ => {}
        }
        return Err(e);
    }

    let local_entries;
    unsafe {
        local_entries = slice::from_raw_parts(local_var_table, num_entries as usize);
    }

    let result = gather_local_information(
        jvmti_env,
        jni_env,
        thread,
        depth,
        method,
        location,
        &local_entries,
    );
    for entry in local_entries {
        let _ = jvmti_env.dealloc(entry.name);
        let _ = jvmti_env.dealloc(entry.signature);
        if !entry.generic_signature.is_null() {
            let _ = jvmti_env.dealloc(entry.generic_signature);
        }
    }
    let _ = jvmti_env.dealloc(local_var_table);
    result
}

fn gather_local_information(
    jvmti_env: &mut JvmTiEnv,
    jni_env: &mut JniEnv,
    thread: jthread,
    depth: jint,
    method: jmethodID,
    location: jlocation,
    local_entries: &[jvmtiLocalVariableEntry],
) -> Result<jobject> {
    let local_class = jni_env.find_class("com/rollbar/jvmti/LocalVariable")?;
    let ctor = jni_env.get_method_id(
        local_class,
        "<init>",
        "(Ljava/lang/String;Ljava/lang/Object;)V",
    )?;
    let locals =
        jni_env.new_object_array(local_entries.len() as jsize, local_class, ptr::null_mut())?;

    for (i, entry) in local_entries.iter().enumerate() {
        make_local_variable(
            jvmti_env,
            jni_env,
            thread,
            depth,
            local_class,
            ctor,
            location,
            locals,
            entry,
            i as jint,
        )?;
    }
    make_frame_object(jvmti_env, jni_env, method, locals)
}

fn make_local_variable(
    jvmti_env: &mut JvmTiEnv,
    jni_env: &mut JniEnv,
    thread: jthread,
    depth: jint,
    local_class: jclass,
    ctor: jmethodID,
    location: jlocation,
    locals: jobjectArray,
    entry: &jvmtiLocalVariableEntry,
    index: jint,
) -> Result<()> {
    let name = jni_env.new_string_utf(entry.name)?;

    let local = if location >= entry.start_location
        && location <= entry.start_location + i64::from(entry.length)
    {
        let value = get_local_value(jvmti_env, jni_env, thread, depth, entry)?;
        jni_env.new_object_StringL(local_class, ctor, name, value)?
    } else {
        ptr::null_mut()
    };
    jni_env.set_object_array_element(locals, index, local)?;
    Ok(())
}

fn make_frame_object(
    jvmti_env: &mut JvmTiEnv,
    jni_env: &mut JniEnv,
    method: jmethodID,
    locals: jobjectArray,
) -> Result<jobject> {
    let mut method_class: jclass = ptr::null_mut();
    jvmti_env.get_method_declaring_class(method, &mut method_class)?;
    let frame_method = jni_env.get_reflected_method(method_class, method, true)?;
    let frame_class = jni_env.find_class("com/rollbar/jvmti/CacheFrame")?;
    let ctor = jni_env.get_method_id(
        frame_class,
        "<init>",
        "(Ljava/lang/reflect/Method;[Lcom/rollbar/jvmti/LocalVariable;)V",
    )?;
    jni_env.new_object_LAL(frame_class, ctor, frame_method, locals)
}

fn get_local_value(
    jvmti_env: &mut JvmTiEnv,
    jni_env: &mut JniEnv,
    thread: jthread,
    depth: jint,
    entry: &jvmtiLocalVariableEntry,
) -> Result<jobject> {
    let signature;
    unsafe {
        signature = CStr::from_ptr(entry.signature).to_bytes();
    }

    if signature.is_empty() {
        let message = "bad local variable signature".to_owned();
        bail!(ErrorKind::Jni(message));
    }
    match signature[0] {
        b'[' | b'L' => {
            let mut result: jobject = ptr::null_mut();
            jvmti_env.get_local_object(thread, depth, entry.slot, &mut result)?;
            Ok(result)
        }
        b'J' => {
            let mut val: jlong = 0;
            jvmti_env.get_local_long(thread, depth, entry.slot, &mut val)?;
            jni_env.value_of("java/lang/Long", "(J)Ljava/lang/Long;", val)
        }
        b'F' => {
            let mut val: jfloat = 0.0;
            jvmti_env.get_local_float(thread, depth, entry.slot, &mut val)?;
            jni_env.value_of("java/lang/Float", "(F)Ljava/lang/Float;", val)
        }
        b'D' => {
            let mut val: jdouble = 0.0;
            jvmti_env.get_local_double(thread, depth, entry.slot, &mut val)?;
            jni_env.value_of("java/lang/Double", "(D)Ljava/lang/Double;", val)
        }
        b'I' => {
            let mut val: jint = 0;
            jvmti_env.get_local_int(thread, depth, entry.slot, &mut val)?;
            jni_env.value_of("java/lang/Integer", "(I)Ljava/lang/Integer;", val)
        }
        b'S' => {
            let mut val: jint = 0;
            jvmti_env.get_local_int(thread, depth, entry.slot, &mut val)?;
            jni_env.value_of("java/lang/Short", "(S)Ljava/lang/Short;", val)
        }
        b'C' => {
            let mut val: jint = 0;
            jvmti_env.get_local_int(thread, depth, entry.slot, &mut val)?;
            jni_env.value_of("java/lang/Character", "(C)Ljava/lang/Character;", val)
        }
        b'B' => {
            let mut val: jint = 0;
            jvmti_env.get_local_int(thread, depth, entry.slot, &mut val)?;
            jni_env.value_of("java/lang/Byte", "(B)Ljava/lang/Byte;", val)
        }
        b'Z' => {
            let mut val: jint = 0;
            jvmti_env.get_local_int(thread, depth, entry.slot, &mut val)?;
            jni_env.value_of("java/lang/Boolean", "(Z)Ljava/lang/Boolean;", val)
        }
        _ => {
            let message = "bad local variable signature".to_owned();
            bail!(ErrorKind::Jni(message))
        }
    }
}
