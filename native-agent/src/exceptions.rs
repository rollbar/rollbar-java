use cache::{self, BoxType, Capture, Core};
use filter;
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

/// Upper bound on how many frames we gather locals for. This bounds the genuinely
/// expensive work -- a local variable table per frame plus a JVMTI call and a boxing
/// allocation per slot -- without truncating the frame array itself, which
/// `BodyFactory.frames()` aligns to the stack from the bottom.
const MAX_LOCALS_FRAMES: jint = 64;

/// Stacks deeper than this are not cached at all. One frame object per stack frame is
/// proportional to depth, and a StackOverflowError fires this event with the stack at
/// maximum depth -- thousands of JNI NewObject calls on a thread that has just run out
/// of stack. Skipping is safe: no locals is what happens without the agent anyway,
/// whereas a truncated array would silently misalign against the Java side.
///
/// 1024 also matches HotSpot's default `MaxJavaStackTraceDepth`, beyond which
/// `Throwable.getStackTrace()` is itself truncated. Past that point the two arrays
/// could not be aligned even if we captured everything.
const MAX_TOTAL_FRAMES: jint = 1024;

const ACC_STATIC: jint = 0x0008;

pub fn inner_callback(
    mut jvmti_env: JvmTiEnv,
    mut jni_env: JniEnv,
    thread: jthread,
    exception: jobject,
) -> Result<()> {
    trace!("on_exception called");

    // Not armed yet: ThrowableCache has not been prepared, so there is nothing to
    // call. The Exception event should not even be enabled in this state.
    let core: &Core = match cache::CORE.get() {
        Some(core) => core,
        None => return Ok(()),
    };

    // Cheap native reject before GetFrameCount (a stack walk) and before the Java
    // upcall, whose shouldCacheThrowable materializes a full StackTraceElement[].
    match filter::decide(&mut jvmti_env, &mut jni_env, core, thread) {
        filter::Decision::Skip | filter::Decision::NoAppFrame => return Ok(()),
        filter::Decision::AskJava => {}
    }

    let num_frames = jvmti_env.get_frame_count(thread)?;

    let should_cache = jni_env.call_static_LI_Z_method(
        core.throwable_cache,
        core.should_cache,
        exception,
        num_frames,
    )?;
    if !should_cache {
        return Ok(());
    }

    if num_frames > MAX_TOTAL_FRAMES {
        debug!(
            "skipping capture: {} frames exceeds the {} frame ceiling",
            num_frames, MAX_TOTAL_FRAMES
        );
        return Ok(());
    }

    let capture = cache::capture(&mut jni_env, core)?;

    // The whole stack is captured. BodyFactory.frames() walks the CacheFrame[] and the
    // StackTraceElement[] together from the bottom, resyncing by method name, so a
    // subset taken from the top would align against the wrong region of the stack.
    let start_depth = 0;
    let frames =
        build_stack_trace_frames(jvmti_env, jni_env, capture, thread, start_depth, num_frames)?;

    jni_env.call_static_LAL_V_method(core.throwable_cache, core.add, exception, frames)?;
    trace!("on_exception exit");
    Ok(())
}

fn build_stack_trace_frames(
    mut jvmti_env: JvmTiEnv,
    mut jni_env: JniEnv,
    capture: &Capture,
    thread: jthread,
    start_depth: jint,
    num_frames: jint,
) -> Result<jobjectArray> {
    if num_frames <= 0 {
        return jni_env.new_object_array(0, capture.cache_frame, ptr::null_mut());
    }

    // GetStackTrace writes into this buffer; it is sized, not filled, up front.
    let mut frames: Vec<jvmtiFrameInfo> = vec![jvmtiFrameInfo::default(); num_frames as usize];
    let mut num_frames_returned: jint = 0;
    jvmti_env.get_stack_trace(
        thread,
        start_depth,
        num_frames,
        frames.as_mut_ptr(),
        &mut num_frames_returned,
    )?;
    if num_frames_returned < 0 {
        num_frames_returned = 0;
    }
    if num_frames_returned as usize > frames.len() {
        num_frames_returned = frames.len() as jint;
    }

    let result =
        jni_env.new_object_array(num_frames_returned, capture.cache_frame, ptr::null_mut())?;
    let mut locals_budget = MAX_LOCALS_FRAMES;
    for i in 0..num_frames_returned {
        let frame = build_frame(
            &mut jvmti_env,
            &mut jni_env,
            capture,
            thread,
            start_depth + i,
            frames[i as usize].method,
            frames[i as usize].location,
            &mut locals_budget,
        )?;
        jni_env.set_object_array_element(result, i, frame)?;
    }
    Ok(result)
}

#[allow(clippy::too_many_arguments)]
fn build_frame(
    jvmti_env: &mut JvmTiEnv,
    jni_env: &mut JniEnv,
    capture: &Capture,
    thread: jthread,
    depth: jint,
    method: jmethodID,
    location: jlocation,
    locals_budget: &mut jint,
) -> Result<jobject> {
    // Locals are only useful for the user's own code, and only worth a bounded amount
    // of work: each one costs a GetLocalVariableTable plus a JVMTI call and a boxing
    // allocation per slot. A frame object is emitted either way so the array stays 1:1
    // with the stack -- BodyFactory.frames() aligns it positionally from the bottom
    // and dereferences every element.
    if *locals_budget <= 0 || !filter::is_app_frame(jvmti_env, method) {
        return make_frame_object(jvmti_env, jni_env, capture, method, ptr::null_mut());
    }
    *locals_budget -= 1;

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
                return make_frame_object(jvmti_env, jni_env, capture, method, ptr::null_mut());
            }
            _ => {}
        }
        return Err(e);
    }

    // from_raw_parts requires a non-null, aligned pointer even for a zero length.
    if num_entries <= 0 || local_var_table.is_null() {
        if !local_var_table.is_null() {
            let _ = jvmti_env.dealloc(local_var_table);
        }
        return make_frame_object(jvmti_env, jni_env, capture, method, ptr::null_mut());
    }

    let local_entries;
    unsafe {
        local_entries = slice::from_raw_parts(local_var_table, num_entries as usize);
    }

    let result = gather_local_information(
        jvmti_env,
        jni_env,
        capture,
        thread,
        depth,
        method,
        location,
        local_entries,
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

#[allow(clippy::too_many_arguments)]
fn gather_local_information(
    jvmti_env: &mut JvmTiEnv,
    jni_env: &mut JniEnv,
    capture: &Capture,
    thread: jthread,
    depth: jint,
    method: jmethodID,
    location: jlocation,
    local_entries: &[jvmtiLocalVariableEntry],
) -> Result<jobject> {
    let locals = jni_env.new_object_array(
        local_entries.len() as jsize,
        capture.local_variable,
        ptr::null_mut(),
    )?;

    for (i, entry) in local_entries.iter().enumerate() {
        make_local_variable(
            jvmti_env, jni_env, capture, thread, depth, location, locals, entry, i as jint,
        )?;
    }
    make_frame_object(jvmti_env, jni_env, capture, method, locals)
}

#[allow(clippy::too_many_arguments)]
fn make_local_variable(
    jvmti_env: &mut JvmTiEnv,
    jni_env: &mut JniEnv,
    capture: &Capture,
    thread: jthread,
    depth: jint,
    location: jlocation,
    locals: jobjectArray,
    entry: &jvmtiLocalVariableEntry,
    index: jint,
) -> Result<()> {
    let in_scope = location >= entry.start_location
        && location <= entry.start_location + i64::from(entry.length);

    // Building the name string only matters for slots we actually emit.
    let local = if in_scope {
        let name = jni_env.new_string_utf(entry.name)?;
        let value = get_local_value(jvmti_env, jni_env, capture, thread, depth, entry)?;
        jni_env.new_object_StringL(capture.local_variable, capture.local_variable_ctor, name, value)?
    } else {
        ptr::null_mut()
    };
    jni_env.set_object_array_element(locals, index, local)?;
    Ok(())
}

fn make_frame_object(
    jvmti_env: &mut JvmTiEnv,
    jni_env: &mut JniEnv,
    capture: &Capture,
    method: jmethodID,
    locals: jobjectArray,
) -> Result<jobject> {
    let mut method_class: jclass = ptr::null_mut();
    jvmti_env.get_method_declaring_class(method, &mut method_class)?;
    let is_static = jvmti_env
        .get_method_modifiers(method)
        .map(|modifiers| modifiers & ACC_STATIC != 0)
        .unwrap_or(false);
    let frame_method = jni_env.get_reflected_method(method_class, method, is_static)?;
    jni_env.new_object_LAL(
        capture.cache_frame,
        capture.cache_frame_ctor,
        frame_method,
        locals,
    )
}

fn get_local_value(
    jvmti_env: &mut JvmTiEnv,
    jni_env: &mut JniEnv,
    capture: &Capture,
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
            box_value(jni_env, &capture.long_box, val)
        }
        b'F' => {
            let mut val: jfloat = 0.0;
            jvmti_env.get_local_float(thread, depth, entry.slot, &mut val)?;
            box_value(jni_env, &capture.float_box, val)
        }
        b'D' => {
            let mut val: jdouble = 0.0;
            jvmti_env.get_local_double(thread, depth, entry.slot, &mut val)?;
            box_value(jni_env, &capture.double_box, val)
        }
        b'I' => {
            let mut val: jint = 0;
            jvmti_env.get_local_int(thread, depth, entry.slot, &mut val)?;
            box_value(jni_env, &capture.int_box, val)
        }
        b'S' => {
            let mut val: jint = 0;
            jvmti_env.get_local_int(thread, depth, entry.slot, &mut val)?;
            box_value(jni_env, &capture.short_box, val)
        }
        b'C' => {
            let mut val: jint = 0;
            jvmti_env.get_local_int(thread, depth, entry.slot, &mut val)?;
            box_value(jni_env, &capture.char_box, val)
        }
        b'B' => {
            let mut val: jint = 0;
            jvmti_env.get_local_int(thread, depth, entry.slot, &mut val)?;
            box_value(jni_env, &capture.byte_box, val)
        }
        b'Z' => {
            let mut val: jint = 0;
            jvmti_env.get_local_int(thread, depth, entry.slot, &mut val)?;
            box_value(jni_env, &capture.bool_box, val)
        }
        _ => {
            let message = "bad local variable signature".to_owned();
            bail!(ErrorKind::Jni(message))
        }
    }
}

fn box_value<T>(jni_env: &mut JniEnv, boxed: &BoxType, val: T) -> Result<jobject> {
    jni_env.value_of_cached(boxed.class, boxed.value_of, val)
}
