use jvmti::jvmtiEnv;
use std::ffi::{CStr, CString};
use std::mem::size_of;
use std::ptr;

use c_on_exception;
use errors::*;

macro_rules! jvmtifn (
    ($r:expr, $f:ident, $($arg:tt)*) => { {
        let rc;
        #[allow(unused_unsafe)] // suppress warning if used inside unsafe block
        unsafe {
            let fnc = (**$r).$f.expect(&format!("{} function not found", stringify!($f)));
            rc = fnc($r, $($arg)*);
        }
        if rc != ::jvmti::jvmtiError_JVMTI_ERROR_NONE {
            let message = format!("JVMTI {} failed", stringify!($f));
            bail!(::errors::ErrorKind::JvmTi(message, rc as i32))
        } else {
            Ok(())
        }
    } }
);

#[derive(Clone, Copy)]
pub struct JvmTiEnv {
    jvmti: *mut jvmtiEnv,
}

impl JvmTiEnv {
    pub fn new(vm: *mut ::jvmti::JavaVM) -> Result<JvmTiEnv> {
        let mut penv: *mut ::std::os::raw::c_void = ptr::null_mut();
        let rc;
        unsafe {
            rc = (**vm).GetEnv.expect("GetEnv function not found")(
                vm,
                &mut penv,
                ::jvmti::JVMTI_VERSION as i32,
            );
        }
        if rc as u32 != ::jvmti::JNI_OK {
            warn!("ERROR: GetEnv failed: {}", rc);
            bail!(ErrorKind::JvmTi("GetEnv failed".into(), ::jvmti::JNI_ERR));
        }

        Ok(JvmTiEnv {
            jvmti: penv as *mut jvmtiEnv,
        })
    }

    pub fn wrap(jvmti_env: *mut jvmtiEnv) -> JvmTiEnv {
        JvmTiEnv { jvmti: jvmti_env }
    }

    pub fn enable_capabilities(&mut self) -> Result<()> {
        let mut capabilities = ::jvmti::jvmtiCapabilities::default();
        capabilities.set_can_generate_exception_events(1u32);
        capabilities.set_can_access_local_variables(1u32);
        jvmtifn!(self.jvmti, AddCapabilities, &capabilities)
    }

    pub fn set_exception_handler(&mut self) -> Result<()> {
        let callbacks = ::jvmti::jvmtiEventCallbacks {
            Exception: Some(c_on_exception),
            ..Default::default()
        };

        // This binding is necessary, the type checker can't handle ? here
        let a: Result<()> = jvmtifn!(
            self.jvmti,
            SetEventCallbacks,
            &callbacks,
            size_of::<::jvmti::jvmtiEventCallbacks>() as i32
        );
        if a.is_err() {
            return a;
        }
        jvmtifn!(
            self.jvmti,
            SetEventNotificationMode,
            ::jvmti::jvmtiEventMode_JVMTI_ENABLE,
            ::jvmti::jvmtiEvent_JVMTI_EVENT_EXCEPTION,
            ::std::ptr::null_mut()
        )
    }

    pub fn get_frame_count(&mut self, thread: ::jvmti::jthread) -> Result<::jvmti::jint> {
        let mut result: ::jvmti::jint = 0;
        let rc;
        unsafe {
            rc = (**self.jvmti)
                .GetFrameCount
                .expect("GetFrameCount function not found")(
                self.jvmti, thread, &mut result
            );
        }
        if rc != ::jvmti::jvmtiError_JVMTI_ERROR_NONE {
            let message = "JVMTI GetFrameCount failed".to_owned();
            bail!(ErrorKind::JvmTi(message, rc as i32))
        } else {
            Ok(result)
        }
    }

    pub fn get_stack_trace(
        &mut self,
        thread: ::jvmti::jthread,
        start_depth: ::jvmti::jint,
        num_frames: ::jvmti::jint,
        frame_buffer: *mut ::jvmti::jvmtiFrameInfo,
        count_ptr: *mut ::jvmti::jint,
    ) -> Result<()> {
        jvmtifn!(
            self.jvmti,
            GetStackTrace,
            thread,
            start_depth,
            num_frames,
            frame_buffer,
            count_ptr
        )
    }

    pub fn get_local_variable_table(
        &mut self,
        method: ::jvmti::jmethodID,
        num_entries: &mut ::jvmti::jint,
        local_var_table: *mut *mut ::jvmti::jvmtiLocalVariableEntry,
    ) -> Result<()> {
        jvmtifn!(
            self.jvmti,
            GetLocalVariableTable,
            method,
            num_entries,
            local_var_table
        )
    }

    pub fn deallocate(&mut self, ptr: *mut ::std::os::raw::c_uchar) -> Result<()> {
        jvmtifn!(self.jvmti, Deallocate, ptr)
    }

    pub fn get_local_object(
        &mut self,
        thread: ::jvmti::jthread,
        depth: ::jvmti::jint,
        slot: ::jvmti::jint,
        result: *mut ::jvmti::jobject,
    ) -> Result<()> {
        jvmtifn!(self.jvmti, GetLocalObject, thread, depth, slot, result)
    }

    pub fn get_local_long(
        &mut self,
        thread: ::jvmti::jthread,
        depth: ::jvmti::jint,
        slot: ::jvmti::jint,
        result: *mut ::jvmti::jlong,
    ) -> Result<()> {
        jvmtifn!(self.jvmti, GetLocalLong, thread, depth, slot, result)
    }

    pub fn get_local_float(
        &mut self,
        thread: ::jvmti::jthread,
        depth: ::jvmti::jint,
        slot: ::jvmti::jint,
        result: *mut ::jvmti::jfloat,
    ) -> Result<()> {
        jvmtifn!(self.jvmti, GetLocalFloat, thread, depth, slot, result)
    }

    pub fn get_local_double(
        &mut self,
        thread: ::jvmti::jthread,
        depth: ::jvmti::jint,
        slot: ::jvmti::jint,
        result: *mut ::jvmti::jdouble,
    ) -> Result<()> {
        jvmtifn!(self.jvmti, GetLocalDouble, thread, depth, slot, result)
    }

    pub fn get_local_int(
        &mut self,
        thread: ::jvmti::jthread,
        depth: ::jvmti::jint,
        slot: ::jvmti::jint,
        result: *mut ::jvmti::jint,
    ) -> Result<()> {
        jvmtifn!(self.jvmti, GetLocalInt, thread, depth, slot, result)
    }

    pub fn get_method_declaring_class(
        &mut self,
        method: ::jvmti::jmethodID,
        result: *mut ::jvmti::jclass,
    ) -> Result<()> {
        jvmtifn!(self.jvmti, GetMethodDeclaringClass, method, result)
    }
}

#[derive(Clone, Copy)]
pub struct JniEnv {
    jni: *mut ::jvmti::JNIEnv,
}

impl JniEnv {
    pub fn new(jni_env: *mut ::jvmti::JNIEnv) -> JniEnv {
        JniEnv { jni: jni_env }
    }

    pub fn call_object_method_internal(
        &mut self,
        object: ::jvmti::jobject,
        method_id: ::jvmti::jmethodID,
    ) -> Option<::jvmti::jobject> {
        let result;
        unsafe {
            result = (**self.jni)
                .CallObjectMethod
                .expect("CallObjectMethod function not found")(
                self.jni, object, method_id
            );
        }
        if result.is_null() {
            None
        } else {
            Some(result)
        }
    }

    pub fn call_static_object_method<T>(
        &mut self,
        class: ::jvmti::jclass,
        method_id: ::jvmti::jmethodID,
        arg: T,
    ) -> Result<::jvmti::jobject> {
        let result;
        unsafe {
            result = (**self.jni)
                .CallStaticObjectMethod
                .expect("CallStaticObjectMethod not found")(
                self.jni, class, method_id, arg
            );
        }
        if self.exception_occurred() || result.is_null() {
            let message = format!(
                "call to static method_id {:?} on class {:?} failed",
                method_id, class
            );
            self.diagnose_exception(&message)?;
            bail!(ErrorKind::Jni(message));
        }
        Ok(result)
    }

    pub fn call_static_LI_Z_method(
        &mut self,
        class: ::jvmti::jclass,
        method_id: ::jvmti::jmethodID,
        arg1: ::jvmti::jobject,
        arg2: ::jvmti::jint,
    ) -> Result<bool> {
        let result;
        unsafe {
            result = (**self.jni)
                .CallStaticBooleanMethod
                .expect("CallStaticBooleanMethod not found")(
                self.jni, class, method_id, arg1, arg2,
            );
        }
        if self.exception_occurred() {
            let message = format!(
                "call to static method_id {:?} on class {:?} failed",
                method_id, class
            );
            self.diagnose_exception(&message)?;
            bail!(ErrorKind::Jni(message));
        }
        Ok(result != 0)
    }

    pub fn call_static_LAL_V_method(
        &mut self,
        class: ::jvmti::jclass,
        method_id: ::jvmti::jmethodID,
        arg1: ::jvmti::jobject,
        arg2: ::jvmti::jobjectArray,
    ) -> Result<()> {
        unsafe {
            (**self.jni)
                .CallStaticVoidMethod
                .expect("CallStaticVoidMethod not found")(
                self.jni, class, method_id, arg1, arg2
            );
        }
        if self.exception_occurred() {
            let message = format!(
                "call to static method_id {:?} on class {:?} failed",
                method_id, class
            );
            self.diagnose_exception(&message)?;
            bail!(ErrorKind::Jni(message));
        }
        Ok(())
    }

    fn exception_occurred(&mut self) -> bool {
        unsafe {
            (**self.jni)
                .ExceptionCheck
                .expect("ExceptionCheck function not found")(self.jni)
                == ::jvmti::JNI_TRUE as u8
        }
    }

    pub fn find_class(&mut self, class_name: &str) -> Result<::jvmti::jclass> {
        let class;
        let c_class_name = CString::new(class_name)?;
        unsafe {
            class = (**self.jni)
                .FindClass
                .expect("FindClass function not found")(
                self.jni, c_class_name.as_ptr()
            )
        }
        if self.exception_occurred() || class.is_null() {
            let message = format!("{} class not found", class_name);
            self.diagnose_exception(&message)?;
            bail!(ErrorKind::Jni(message))
        } else {
            Ok(class)
        }
    }

    pub fn get_static_method_id(
        &mut self,
        class: ::jvmti::jclass,
        method: &str,
        signature: &str,
    ) -> Result<::jvmti::jmethodID> {
        let c_method = CString::new(method)?;
        let c_signature = CString::new(signature)?;
        let method_id = self.get_static_method_id_internal(class, &c_method, &c_signature);
        if self.exception_occurred() || method_id == None {
            let message = format!(
                "{} static method with signature {} not found",
                method, signature
            );
            self.diagnose_exception(&message)?;
            bail!(ErrorKind::Jni(message));
        }
        Ok(method_id.expect("impossible error"))
    }

    fn get_static_method_id_internal(
        &mut self,
        class: ::jvmti::jclass,
        method: &CString,
        signature: &CString,
    ) -> Option<::jvmti::jmethodID> {
        let method_id;
        unsafe {
            method_id = (**self.jni)
                .GetStaticMethodID
                .expect("GetStaticMethodID function not found")(
                self.jni,
                class,
                method.as_ptr(),
                signature.as_ptr(),
            );
        }
        if method_id.is_null() {
            None
        } else {
            Some(method_id)
        }
    }

    pub fn new_object_StringL(
        &mut self,
        class: ::jvmti::jclass,
        ctor: ::jvmti::jmethodID,
        arg1: ::jvmti::jstring,
        arg2: ::jvmti::jobject,
    ) -> Result<::jvmti::jobject> {
        let result;
        unsafe {
            result = (**self.jni)
                .NewObject
                .expect("NewObject function not found")(
                self.jni, class, ctor, arg1, arg2
            );
        }
        if self.exception_occurred() || result.is_null() {
            let message = "object creation failed [StringL]".to_owned();
            self.diagnose_exception(&message)?;
            bail!(ErrorKind::Jni(message))
        } else {
            Ok(result)
        }
    }

    pub fn new_object_LAL(
        &mut self,
        class: ::jvmti::jclass,
        ctor: ::jvmti::jmethodID,
        arg1: ::jvmti::jobject,
        arg2: ::jvmti::jobjectArray,
    ) -> Result<::jvmti::jobject> {
        let result;
        unsafe {
            result = (**self.jni)
                .NewObject
                .expect("NewObject function not found")(
                self.jni, class, ctor, arg1, arg2
            );
        }
        if self.exception_occurred() || result.is_null() {
            let message = "object creation failed [LAL]".to_owned();
            self.diagnose_exception(&message)?;
            bail!(ErrorKind::Jni(message))
        } else {
            Ok(result)
        }
    }

    pub fn new_object_array(
        &mut self,
        length: ::jvmti::jsize,
        class: ::jvmti::jclass,
        init: ::jvmti::jobject,
    ) -> Result<::jvmti::jobjectArray> {
        let result;
        unsafe {
            result = (**self.jni)
                .NewObjectArray
                .expect("NewObjectArray function not found")(
                self.jni, length, class, init
            );
        }
        if self.exception_occurred() || result.is_null() {
            let message = "object array creation failed".to_owned();
            self.diagnose_exception(&message)?;
            bail!(ErrorKind::Jni(message))
        } else {
            Ok(result)
        }
    }

    pub fn set_object_array_element(
        &mut self,
        array: ::jvmti::jobjectArray,
        index: ::jvmti::jsize,
        val: ::jvmti::jobject,
    ) -> Result<()> {
        unsafe {
            (**self.jni)
                .SetObjectArrayElement
                .expect("SetObjectArrayElement function nout found")(
                self.jni, array, index, val
            );
        }
        if self.exception_occurred() {
            let message = "object array element set failed".to_owned();
            self.diagnose_exception(&message)?;
            bail!(ErrorKind::Jni(message))
        } else {
            Ok(())
        }
    }

    pub fn get_method_id(
        &mut self,
        class: ::jvmti::jclass,
        method: &str,
        signature: &str,
    ) -> Result<::jvmti::jmethodID> {
        let c_method = CString::new(method)?;
        let c_signature = CString::new(signature)?;
        let method_id = self.get_method_id_internal(class, &c_method, &c_signature);
        if self.exception_occurred() || method_id == None {
            let message = format!("{} method with signature {} not found", method, signature);
            self.diagnose_exception(&message)?;
            bail!(ErrorKind::Jni(message));
        }

        Ok(method_id.expect("unexpected error"))
    }

    fn get_method_id_internal(
        &mut self,
        class: ::jvmti::jclass,
        method: &CString,
        signature: &CString,
    ) -> Option<::jvmti::jmethodID> {
        let method_id;
        unsafe {
            method_id = (**self.jni)
                .GetMethodID
                .expect("GetMethodID function not found")(
                self.jni,
                class,
                method.as_ptr(),
                signature.as_ptr(),
            );
        }
        if method_id.is_null() {
            None
        } else {
            Some(method_id)
        }
    }

    fn get_object_class_internal(&mut self, object: ::jvmti::jobject) -> Option<::jvmti::jclass> {
        let class;
        unsafe {
            class = (**self.jni)
                .GetObjectClass
                .expect("GetObjectClass function not found")(self.jni, object)
        }
        if class.is_null() {
            None
        } else {
            Some(class)
        }
    }

    pub fn get_exception_message(&mut self, exc: ::jvmti::jobject) -> Option<String> {
        self.get_object_class_internal(exc)
            .and_then(|exc_class| {
                let c_method = match CString::new("getMessage") {
                    Ok(s) => s,
                    Err(_) => return None,
                };
                let c_signature = match CString::new("()Ljava/lang/String;") {
                    Ok(s) => s,
                    Err(_) => return None,
                };
                self.get_method_id_internal(exc_class, &c_method, &c_signature)
            }).and_then(|method_id| self.call_object_method_returning_string(exc, method_id))
    }

    fn call_object_method_returning_string(
        &mut self,
        obj: ::jvmti::jobject,
        method: ::jvmti::jmethodID,
    ) -> Option<String> {
        let result = match self.call_object_method_internal(obj, method) {
            Some(s) => s as ::jvmti::jstring,
            None => return None,
        };
        let (result_utf_chars, result_cstr) = self.get_string_utf_chars(result);
        let rust_result = result_cstr.to_string_lossy().into_owned();
        self.release_string_utf_chars(result, result_utf_chars);
        Some(rust_result)
    }

    pub fn diagnose_exception(&mut self, message: &str) -> Result<()> {
        if !self.exception_occurred() {
            return Ok(());
        }
        let exc;
        unsafe {
            exc = (**self.jni)
                .ExceptionOccurred
                .expect("ExceptionOccurred function not found")(self.jni);
        }

        let exc_message = self
            .get_exception_message(exc)
            .unwrap_or_else(|| "BAD EXCEPTION MESSAGE".to_owned());
        let err = Err(ErrorKind::Jni(format!("{}: {}", message.to_owned(), exc_message)).into());
        unsafe {
            (**self.jni)
                .ExceptionClear
                .expect("ExceptionClear function not found")(self.jni);
        }
        err
    }

    pub fn get_string_utf_chars<'a>(
        &mut self,
        s: ::jvmti::jstring,
    ) -> (*const ::std::os::raw::c_char, &'a CStr) {
        let utf_chars;
        let cstr;
        unsafe {
            utf_chars = (**self.jni)
                .GetStringUTFChars
                .expect("GetStringUTFChars function not found")(
                self.jni, s, ptr::null_mut()
            );
            cstr = CStr::from_ptr(utf_chars);
        }

        (utf_chars, cstr)
    }

    pub fn release_string_utf_chars(
        &mut self,
        s: ::jvmti::jstring,
        utf_chars: *const ::std::os::raw::c_char,
    ) {
        unsafe {
            (**self.jni)
                .ReleaseStringUTFChars
                .expect("ReleaseStringUTFChars function not found")(
                self.jni, s, utf_chars
            );
        }
    }

    pub fn new_string_utf(
        &mut self,
        utf_chars: *const ::std::os::raw::c_char,
    ) -> Result<::jvmti::jstring> {
        let result;
        unsafe {
            result = (**self.jni)
                .NewStringUTF
                .expect("NewStringUTF function not found")(self.jni, utf_chars);
        }
        if self.exception_occurred() || result.is_null() {
            let message = "new string utf failed".to_owned();
            self.diagnose_exception(&message)?;
            bail!(ErrorKind::Jni(message))
        } else {
            Ok(result)
        }
    }

    pub fn get_reflected_method(
        &mut self,
        method_class: ::jvmti::jclass,
        method: ::jvmti::jmethodID,
        is_static: bool,
    ) -> Result<::jvmti::jobject> {
        let result;
        unsafe {
            result = (**self.jni)
                .ToReflectedMethod
                .expect("ToReflectedMethod function not found")(
                self.jni,
                method_class,
                method,
                is_static as ::std::os::raw::c_uchar,
            );
        }
        if self.exception_occurred() || result.is_null() {
            let message = "new string utf failed".to_owned();
            self.diagnose_exception(&message)?;
            bail!(ErrorKind::Jni(message))
        } else {
            Ok(result)
        }
    }
}

pub fn inner_callback(
    mut jvmti_env: JvmTiEnv,
    mut jni_env: JniEnv,
    thread: ::jvmti::jthread,
    exception: ::jvmti::jobject,
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
    thread: ::jvmti::jthread,
    start_depth: ::jvmti::jint,
    num_frames: ::jvmti::jint,
) -> Result<::jvmti::jobjectArray> {
    let mut frames: Vec<::jvmti::jvmtiFrameInfo> = Vec::with_capacity(num_frames as usize);
    let mut num_frames_returned: ::jvmti::jint = 0;
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

// TODO: Make sure local_var_table gets deallocated in case of early return
// due to error. Probably via an inner wrapper function.
fn build_frame(
    jvmti_env: &mut JvmTiEnv,
    jni_env: &mut JniEnv,
    thread: ::jvmti::jthread,
    depth: ::jvmti::jint,
    method: ::jvmti::jmethodID,
    location: ::jvmti::jlocation,
) -> Result<::jvmti::jobject> {
    let mut num_entries: ::jvmti::jint = 0;
    let mut local_var_table: *mut ::jvmti::jvmtiLocalVariableEntry = ptr::null_mut();

    if let Err(e) =
        jvmti_env.get_local_variable_table(method, &mut num_entries, &mut local_var_table)
    {
        match e {
            Error(ErrorKind::JvmTi(_, rc), _)
                if rc == ::jvmti::jvmtiError_JVMTI_ERROR_ABSENT_INFORMATION as ::jvmti::jint
                    || rc == ::jvmti::jvmtiError_JVMTI_ERROR_NATIVE_METHOD as ::jvmti::jint =>
            {
                return make_frame_object(jvmti_env, jni_env, method, ptr::null_mut());
            }
            _ => {}
        }
        return Err(e);
    }

    let local_class = jni_env.find_class("com/rollbar/jvmti/LocalVariable")?;
    let ctor = jni_env.get_method_id(
        local_class,
        "<init>",
        "(Ljava/lang/String;Ljava/lang/Object;)V",
    )?;
    let locals = jni_env.new_object_array(num_entries, local_class, ptr::null_mut())?;

    for i in 0..num_entries {
        make_local_variable(
            jvmti_env,
            jni_env,
            thread,
            depth,
            local_class,
            ctor,
            location,
            locals,
            local_var_table,
            i,
        )?;
    }
    jvmti_env.deallocate(local_var_table as *mut ::std::os::raw::c_uchar)?;

    make_frame_object(jvmti_env, jni_env, method, locals)
}

fn make_local_variable(
    jvmti_env: &mut JvmTiEnv,
    jni_env: &mut JniEnv,
    thread: ::jvmti::jthread,
    depth: ::jvmti::jint,
    local_class: ::jvmti::jclass,
    ctor: ::jvmti::jmethodID,
    location: ::jvmti::jlocation,
    locals: ::jvmti::jobjectArray,
    local_var_table: *mut ::jvmti::jvmtiLocalVariableEntry,
    index: ::jvmti::jint,
) -> Result<()> {
    let entry;
    unsafe {
        entry = *local_var_table.offset(index as isize);
    }
    let name = jni_env.new_string_utf(entry.name)?;

    let local = if location >= entry.start_location
        && location <= entry.start_location + i64::from(entry.length)
    {
        let value = get_local_value(jvmti_env, jni_env, thread, depth, local_var_table, index)?;
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
    method: ::jvmti::jmethodID,
    locals: ::jvmti::jobjectArray,
) -> Result<::jvmti::jobject> {
    let mut method_class: ::jvmti::jclass = ptr::null_mut();
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
    thread: ::jvmti::jthread,
    depth: ::jvmti::jint,
    local_var_table: *mut ::jvmti::jvmtiLocalVariableEntry,
    index: ::jvmti::jint,
) -> Result<::jvmti::jobject> {
    let entry;
    let signature;
    unsafe {
        entry = *local_var_table.offset(index as isize);
        signature = CStr::from_ptr(entry.signature).to_bytes();
    }

    if signature.is_empty() {
        let message = "bad local variable signature".to_owned();
        bail!(ErrorKind::Jni(message));
    }
    match signature[0] {
        b'[' | b'L' => {
            let mut result: ::jvmti::jobject = ptr::null_mut();
            jvmti_env.get_local_object(thread, depth, entry.slot, &mut result)?;
            return Ok(result);
        }
        b'J' => {
            let mut val: ::jvmti::jlong = 0;
            jvmti_env.get_local_long(thread, depth, entry.slot, &mut val)?;
            let reflect_class = jni_env.find_class("java/lang/Long")?;
            let value_of =
                jni_env.get_static_method_id(reflect_class, "valueOf", "(J)Ljava/lang/Long;")?;
            return jni_env.call_static_object_method(reflect_class, value_of, val);
        }
        b'F' => {
            let mut val: ::jvmti::jfloat = 0.0;
            jvmti_env.get_local_float(thread, depth, entry.slot, &mut val)?;
            let reflect_class = jni_env.find_class("java/lang/Float")?;
            let value_of =
                jni_env.get_static_method_id(reflect_class, "valueOf", "(F)Ljava/lang/Float;")?;
            return jni_env.call_static_object_method(reflect_class, value_of, val);
        }
        b'D' => {
            let mut val: ::jvmti::jdouble = 0.0;
            jvmti_env.get_local_double(thread, depth, entry.slot, &mut val)?;
            let reflect_class = jni_env.find_class("java/lang/Double")?;
            let value_of =
                jni_env.get_static_method_id(reflect_class, "valueOf", "(D)Ljava/lang/Double;")?;
            return jni_env.call_static_object_method(reflect_class, value_of, val);
        }
        b'I' => {
            let mut val: ::jvmti::jint = 0;
            jvmti_env.get_local_int(thread, depth, entry.slot, &mut val)?;
            let reflect_class = jni_env.find_class("java/lang/Integer")?;
            let value_of =
                jni_env.get_static_method_id(reflect_class, "valueOf", "(I)Ljava/lang/Integer;")?;
            return jni_env.call_static_object_method(reflect_class, value_of, val);
        }
        b'S' => {
            let mut val: ::jvmti::jint = 0;
            jvmti_env.get_local_int(thread, depth, entry.slot, &mut val)?;
            let reflect_class = jni_env.find_class("java/lang/Short")?;
            let value_of =
                jni_env.get_static_method_id(reflect_class, "valueOf", "(S)Ljava/lang/Short;")?;
            return jni_env.call_static_object_method(reflect_class, value_of, val);
        }
        b'C' => {
            let mut val: ::jvmti::jint = 0;
            jvmti_env.get_local_int(thread, depth, entry.slot, &mut val)?;
            let reflect_class = jni_env.find_class("java/lang/Character")?;
            let value_of = jni_env.get_static_method_id(
                reflect_class,
                "valueOf",
                "(C)Ljava/lang/Character;",
            )?;
            return jni_env.call_static_object_method(reflect_class, value_of, val);
        }
        b'B' => {
            let mut val: ::jvmti::jint = 0;
            jvmti_env.get_local_int(thread, depth, entry.slot, &mut val)?;
            let reflect_class = jni_env.find_class("java/lang/Byte")?;
            let value_of =
                jni_env.get_static_method_id(reflect_class, "valueOf", "(B)Ljava/lang/Byte;")?;
            return jni_env.call_static_object_method(reflect_class, value_of, val);
        }
        b'Z' => {
            let mut val: ::jvmti::jint = 0;
            jvmti_env.get_local_int(thread, depth, entry.slot, &mut val)?;
            let reflect_class = jni_env.find_class("java/lang/Boolean")?;
            let value_of =
                jni_env.get_static_method_id(reflect_class, "valueOf", "(Z)Ljava/lang/Boolean;")?;
            return jni_env.call_static_object_method(reflect_class, value_of, val);
        }
        _ => {}
    }
    let message = "bad local variable signature".to_owned();
    bail!(ErrorKind::Jni(message));
}
