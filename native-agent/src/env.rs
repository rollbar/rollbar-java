use std::mem::size_of;
use std::ptr;

use c_on_exception;
use errors::*;

macro_rules! jvmtifn (
    ($r:expr, $f:ident, $($arg:tt)*) => { {
        let rc;
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
    jvmti: *mut ::jvmti::jvmtiEnv,
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
            jvmti: penv as *mut ::jvmti::jvmtiEnv,
        })
    }

    pub fn wrap(jvmti_env: *mut ::jvmti::jvmtiEnv) -> JvmTiEnv {
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
            ptr::null_mut()
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

    pub fn dealloc<T>(&mut self, ptr: *mut T) -> Result<()> {
        jvmtifn!(self.jvmti, Deallocate, ptr as *mut ::std::os::raw::c_uchar)
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
