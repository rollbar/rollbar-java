use std::ffi::CStr;
use std::mem::size_of;
use std::os::raw::{c_char, c_uchar, c_void};
use std::ptr;

use errors::*;

macro_rules! jvmtifn (
    ($r:expr, $f:ident, $($arg:tt)*) => { {
        let rc;
        unsafe {
            let fnc = (**$r).$f.expect(&format!("{} function not found", stringify!($f)));
            rc = fnc($r, $($arg)*);
        }
        if rc != jvmtiError_JVMTI_ERROR_NONE {
            let message = format!("JVMTI {} failed", stringify!($f));
            bail!(::errors::ErrorKind::JvmTi(message, rc as i32))
        } else {
            Ok(())
        }
    } }
);

use jvmti::{
    jclass, jdouble, jfloat, jint, jlong, jmethodID, jobject, jthread, jvmtiCapabilities, jvmtiEnv,
    jvmtiError_JVMTI_ERROR_NONE, jvmtiEvent, jvmtiEventCallbacks, jvmtiEventMode, jvmtiFrameInfo,
    jvmtiLocalVariableEntry, JavaVM, JNI_ERR, JNI_OK, JVMTI_VERSION_1_2,
};

/// JvmTiEnv is a wrapper around the JVMTI environment which is obtained
/// by calling GetEnv with a given, running JavaVM instance. Not being
/// able to obtain the interior pointer here from GetEnv means we cannot
/// do anything and is fatal for this agent.
///
/// The methods defined on this wrapper are in almost all cases encapsulating
/// unsafe calls to the JVMTI C interface and converting error codes into
/// Results. Every effort was made to ensure safety around the necessary unsafe
/// blocks for calling into the FFI.
#[derive(Clone, Copy)]
pub struct JvmTiEnv {
    jvmti: *mut jvmtiEnv,
}

impl JvmTiEnv {
    pub fn new(vm: *mut JavaVM) -> Result<JvmTiEnv> {
        let mut penv: *mut c_void = ptr::null_mut();
        let rc;
        unsafe {
            // Request 1.2 explicitly rather than the JVMTI_VERSION baked in by bindgen
            // at build time: that constant tracks the JDK the agent was *compiled*
            // against, so a JDK 21 build would be rejected with JNI_EVERSION by a
            // JDK 17 runtime. Nothing here needs anything newer than 1.2.
            rc = (**vm).GetEnv.expect("GetEnv function not found")(
                vm,
                &mut penv,
                JVMTI_VERSION_1_2 as i32,
            );
        }
        if rc as u32 != JNI_OK {
            warn!("ERROR: GetEnv failed: {}", rc);
            bail!(ErrorKind::JvmTi("GetEnv failed".into(), JNI_ERR));
        }

        Ok(JvmTiEnv {
            jvmti: penv as *mut jvmtiEnv,
        })
    }

    pub fn wrap(jvmti_env: *mut jvmtiEnv) -> JvmTiEnv {
        JvmTiEnv { jvmti: jvmti_env }
    }

    pub fn enable_capabilities(&mut self) -> Result<()> {
        let mut capabilities = jvmtiCapabilities::default();
        capabilities.set_can_generate_exception_events(1u32);
        capabilities.set_can_access_local_variables(1u32);
        jvmtifn!(self.jvmti, AddCapabilities, &capabilities)
    }

    /// Registers the whole callback table in one shot. `SetEventCallbacks` replaces
    /// the entire struct, so every callback the agent will ever use must be passed
    /// here; delivery is then controlled independently via `set_event_mode`.
    pub fn set_event_callbacks(&mut self, callbacks: &jvmtiEventCallbacks) -> Result<()> {
        jvmtifn!(
            self.jvmti,
            SetEventCallbacks,
            callbacks,
            size_of::<jvmtiEventCallbacks>() as i32
        )
    }

    /// Enables or disables one event globally (all threads).
    pub fn set_event_mode(&mut self, mode: jvmtiEventMode, event: jvmtiEvent) -> Result<()> {
        jvmtifn!(
            self.jvmti,
            SetEventNotificationMode,
            mode,
            event,
            ptr::null_mut()
        )
    }

    /// Compares a class signature without allocating a Rust `String`. This runs for
    /// every class the VM prepares until the agent arms, so it stays on the cheap path.
    pub fn class_signature_is(&mut self, klass: jclass, expected: &CStr) -> Result<bool> {
        let mut sig: *mut c_char = ptr::null_mut();
        // The macro needs a typed binding; `?` alone cannot infer the error type.
        let rc: Result<()> = jvmtifn!(
            self.jvmti,
            GetClassSignature,
            klass,
            &mut sig,
            ptr::null_mut()
        );
        rc?;
        if sig.is_null() {
            return Ok(false);
        }
        let matches = unsafe { CStr::from_ptr(sig) } == expected;
        let _ = self.dealloc(sig);
        Ok(matches)
    }

    /// Returns a class signature (e.g. `Lcom/foo/Bar;`) as owned bytes.
    pub fn get_class_signature(&mut self, klass: jclass) -> Result<Vec<u8>> {
        let mut sig: *mut c_char = ptr::null_mut();
        let rc: Result<()> = jvmtifn!(
            self.jvmti,
            GetClassSignature,
            klass,
            &mut sig,
            ptr::null_mut()
        );
        rc?;
        if sig.is_null() {
            return Ok(Vec::new());
        }
        let owned = unsafe { CStr::from_ptr(sig) }.to_bytes().to_vec();
        let _ = self.dealloc(sig);
        Ok(owned)
    }

    /// The defining loader of `klass`, or null for the bootstrap loader.
    pub fn get_class_loader(&mut self, klass: jclass) -> Result<jobject> {
        let mut loader: jobject = ptr::null_mut();
        let rc: Result<()> = jvmtifn!(self.jvmti, GetClassLoader, klass, &mut loader);
        rc?;
        Ok(loader)
    }

    pub fn get_method_modifiers(&mut self, method: jmethodID) -> Result<jint> {
        let mut modifiers: jint = 0;
        let rc: Result<()> = jvmtifn!(self.jvmti, GetMethodModifiers, method, &mut modifiers);
        rc?;
        Ok(modifiers)
    }

    pub fn get_frame_count(&mut self, thread: jthread) -> Result<jint> {
        let mut result: jint = 0;
        let rc;
        unsafe {
            rc = (**self.jvmti)
                .GetFrameCount
                .expect("GetFrameCount function not found")(
                self.jvmti, thread, &mut result
            );
        }
        if rc != jvmtiError_JVMTI_ERROR_NONE {
            let message = "JVMTI GetFrameCount failed".to_owned();
            bail!(ErrorKind::JvmTi(message, rc as i32))
        } else {
            Ok(result)
        }
    }

    pub fn get_stack_trace(
        &mut self,
        thread: jthread,
        start_depth: jint,
        num_frames: jint,
        frame_buffer: *mut jvmtiFrameInfo,
        count_ptr: *mut jint,
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
        method: jmethodID,
        num_entries: &mut jint,
        local_var_table: *mut *mut jvmtiLocalVariableEntry,
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
        jvmtifn!(self.jvmti, Deallocate, ptr as *mut c_uchar)
    }

    pub fn get_local_object(
        &mut self,
        thread: jthread,
        depth: jint,
        slot: jint,
        result: *mut jobject,
    ) -> Result<()> {
        jvmtifn!(self.jvmti, GetLocalObject, thread, depth, slot, result)
    }

    pub fn get_local_long(
        &mut self,
        thread: jthread,
        depth: jint,
        slot: jint,
        result: *mut jlong,
    ) -> Result<()> {
        jvmtifn!(self.jvmti, GetLocalLong, thread, depth, slot, result)
    }

    pub fn get_local_float(
        &mut self,
        thread: jthread,
        depth: jint,
        slot: jint,
        result: *mut jfloat,
    ) -> Result<()> {
        jvmtifn!(self.jvmti, GetLocalFloat, thread, depth, slot, result)
    }

    pub fn get_local_double(
        &mut self,
        thread: jthread,
        depth: jint,
        slot: jint,
        result: *mut jdouble,
    ) -> Result<()> {
        jvmtifn!(self.jvmti, GetLocalDouble, thread, depth, slot, result)
    }

    pub fn get_local_int(
        &mut self,
        thread: jthread,
        depth: jint,
        slot: jint,
        result: *mut jint,
    ) -> Result<()> {
        jvmtifn!(self.jvmti, GetLocalInt, thread, depth, slot, result)
    }

    pub fn get_method_declaring_class(
        &mut self,
        method: jmethodID,
        result: *mut jclass,
    ) -> Result<()> {
        jvmtifn!(self.jvmti, GetMethodDeclaringClass, method, result)
    }
}
