use errors::*;
use std::ffi::{CStr, CString};
use std::ptr;

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
            let message = "reflected method lookup failed".to_owned();
            self.diagnose_exception(&message)?;
            bail!(ErrorKind::Jni(message))
        } else {
            Ok(result)
        }
    }

    /// Boxes a primitive using an already-resolved class/`valueOf` pair. The
    /// uncached `value_of` did a `FindClass` + `GetStaticMethodID` per local
    /// variable, which dominated the capture path.
    pub fn value_of_cached<T>(
        &mut self,
        class: ::jvmti::jclass,
        value_of: ::jvmti::jmethodID,
        val: T,
    ) -> Result<::jvmti::jobject> {
        self.call_static_object_method(class, value_of, val)
    }

    /// Clears any pending exception without describing it. The description path
    /// (`diagnose_exception`) costs a Java upcall, so it must stay off the hot path,
    /// but the clear itself is mandatory or the next JNI call misbehaves.
    pub fn clear_pending_exception(&mut self) {
        if !self.exception_occurred() {
            return;
        }
        unsafe {
            (**self.jni)
                .ExceptionClear
                .expect("ExceptionClear function not found")(self.jni);
        }
    }

    pub fn new_global_ref(&mut self, obj: ::jvmti::jobject) -> Result<::jvmti::jobject> {
        if obj.is_null() {
            return Ok(ptr::null_mut());
        }
        let result;
        unsafe {
            result = (**self.jni)
                .NewGlobalRef
                .expect("NewGlobalRef function not found")(self.jni, obj);
        }
        if result.is_null() {
            bail!(ErrorKind::Jni("NewGlobalRef failed".to_owned()))
        }
        Ok(result)
    }

    pub fn delete_global_ref(&mut self, obj: ::jvmti::jobject) {
        if obj.is_null() {
            return;
        }
        unsafe {
            (**self.jni)
                .DeleteGlobalRef
                .expect("DeleteGlobalRef function not found")(self.jni, obj);
        }
    }

    /// Bounds the local references created during one callback. Capturing a deep
    /// stack creates thousands of refs; without a frame they all stay live until
    /// the callback returns.
    pub fn push_local_frame(&mut self, capacity: ::jvmti::jint) -> Result<()> {
        let rc;
        unsafe {
            rc = (**self.jni)
                .PushLocalFrame
                .expect("PushLocalFrame function not found")(self.jni, capacity);
        }
        if rc != 0 {
            self.clear_pending_exception();
            bail!(ErrorKind::Jni("PushLocalFrame failed".to_owned()))
        }
        Ok(())
    }

    pub fn pop_local_frame(&mut self) {
        unsafe {
            (**self.jni)
                .PopLocalFrame
                .expect("PopLocalFrame function not found")(self.jni, ptr::null_mut());
        }
    }

    pub fn new_string_utf_str(&mut self, s: &str) -> Result<::jvmti::jstring> {
        let c = CString::new(s)?;
        self.new_string_utf(c.as_ptr())
    }

    /// Resolves a static field, returning `None` (rather than an error) when the
    /// field is absent so the agent stays compatible with older rollbar-java jars.
    pub fn get_static_field_id_opt(
        &mut self,
        class: ::jvmti::jclass,
        name: &str,
        signature: &str,
    ) -> Option<::jvmti::jfieldID> {
        let c_name = match CString::new(name) {
            Ok(s) => s,
            Err(_) => return None,
        };
        let c_signature = match CString::new(signature) {
            Ok(s) => s,
            Err(_) => return None,
        };
        let field_id;
        unsafe {
            field_id = (**self.jni)
                .GetStaticFieldID
                .expect("GetStaticFieldID function not found")(
                self.jni,
                class,
                c_name.as_ptr(),
                c_signature.as_ptr(),
            );
        }
        // A missing field leaves a pending NoSuchFieldError that must be cleared.
        self.clear_pending_exception();
        if field_id.is_null() {
            None
        } else {
            Some(field_id)
        }
    }

    /// Resolves a static method, returning `None` rather than an error when absent,
    /// so the agent stays compatible with older rollbar-java jars.
    pub fn get_static_method_id_opt(
        &mut self,
        class: ::jvmti::jclass,
        method: &str,
        signature: &str,
    ) -> Option<::jvmti::jmethodID> {
        let c_method = CString::new(method).ok()?;
        let c_signature = CString::new(signature).ok()?;
        let method_id = self.get_static_method_id_internal(class, &c_method, &c_signature);
        // A missing method leaves a pending NoSuchMethodError that must be cleared.
        self.clear_pending_exception();
        method_id
    }

    pub fn get_static_int_field(
        &mut self,
        class: ::jvmti::jclass,
        field: ::jvmti::jfieldID,
    ) -> ::jvmti::jint {
        unsafe {
            (**self.jni)
                .GetStaticIntField
                .expect("GetStaticIntField function not found")(self.jni, class, field)
        }
    }

    pub fn call_static_object_method0(
        &mut self,
        class: ::jvmti::jclass,
        method_id: ::jvmti::jmethodID,
    ) -> Result<::jvmti::jobject> {
        let result;
        unsafe {
            result = (**self.jni)
                .CallStaticObjectMethod
                .expect("CallStaticObjectMethod not found")(self.jni, class, method_id);
        }
        if self.exception_occurred() || result.is_null() {
            let message = "static no-arg call failed".to_owned();
            self.diagnose_exception(&message)?;
            bail!(ErrorKind::Jni(message));
        }
        Ok(result)
    }

    pub fn get_array_length(&mut self, array: ::jvmti::jarray) -> ::jvmti::jsize {
        unsafe {
            (**self.jni)
                .GetArrayLength
                .expect("GetArrayLength function not found")(self.jni, array)
        }
    }

    pub fn get_object_array_element(
        &mut self,
        array: ::jvmti::jobjectArray,
        index: ::jvmti::jsize,
    ) -> ::jvmti::jobject {
        unsafe {
            (**self.jni)
                .GetObjectArrayElement
                .expect("GetObjectArrayElement function not found")(self.jni, array, index)
        }
    }

    /// Copies a Java string out as owned bytes.
    pub fn string_to_bytes(&mut self, s: ::jvmti::jstring) -> Option<Vec<u8>> {
        if s.is_null() {
            return None;
        }
        let (utf_chars, cstr) = self.get_string_utf_chars(s);
        if utf_chars.is_null() {
            return None;
        }
        let bytes = cstr.to_bytes().to_vec();
        self.release_string_utf_chars(s, utf_chars);
        Some(bytes)
    }

    /// `Class.forName(name, false, loader)`. Used to resolve `CacheFrame` and
    /// `LocalVariable` through `ThrowableCache`'s own loader; plain `FindClass`
    /// resolves against the system loader and so fails inside a Spring Boot fat jar.
    pub fn class_for_name(
        &mut self,
        class_class: ::jvmti::jclass,
        for_name: ::jvmti::jmethodID,
        name: &str,
        loader: ::jvmti::jobject,
    ) -> Result<::jvmti::jclass> {
        let j_name = self.new_string_utf_str(name)?;
        let result;
        unsafe {
            result = (**self.jni)
                .CallStaticObjectMethod
                .expect("CallStaticObjectMethod not found")(
                self.jni,
                class_class,
                for_name,
                j_name,
                // jboolean is u8, but C varargs promote it to int.
                ::jvmti::JNI_FALSE as ::std::os::raw::c_int,
                loader,
            );
        }
        if self.exception_occurred() || result.is_null() {
            let message = format!("Class.forName({}) failed", name);
            self.diagnose_exception(&message)?;
            bail!(ErrorKind::Jni(message))
        }
        Ok(result as ::jvmti::jclass)
    }
}
