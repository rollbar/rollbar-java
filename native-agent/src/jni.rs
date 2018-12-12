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
            let message = "new string utf failed".to_owned();
            self.diagnose_exception(&message)?;
            bail!(ErrorKind::Jni(message))
        } else {
            Ok(result)
        }
    }

    pub fn value_of<T>(
        &mut self,
        class: &str,
        signature: &str,
        val: T,
    ) -> Result<::jvmti::jobject> {
        let reflect_class = self.find_class(class)?;
        let value_of = self.get_static_method_id(reflect_class, "valueOf", signature)?;
        self.call_static_object_method(reflect_class, value_of, val)
    }
}
