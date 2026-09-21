#![allow(non_upper_case_globals)]
#![allow(non_camel_case_types)]
#![allow(non_snake_case)]

extern crate pretty_env_logger;
#[macro_use]
extern crate log;
#[macro_use]
extern crate error_chain;

mod cache;
mod env;
mod errors;
mod exceptions;
mod filter;
mod jni;

mod jvmti;

use env::JvmTiEnv;
use jni::JniEnv;
use jvmti::{
    jclass, jint, jlocation, jmethodID, jobject, jthread, jvmtiEnv, jvmtiEventCallbacks,
    jvmtiEventMode_JVMTI_DISABLE, jvmtiEventMode_JVMTI_ENABLE,
    jvmtiEvent_JVMTI_EVENT_CLASS_PREPARE, jvmtiEvent_JVMTI_EVENT_EXCEPTION, JNIEnv, JavaVM,
};
use std::cell::Cell;
use std::ffi::CStr;
use std::os::raw::{c_char, c_void};
use std::sync::atomic::{AtomicBool, Ordering};

static INIT_SUCCESS: AtomicBool = AtomicBool::new(false);

/// Number of JNI local references the capture path is expected to need before it
/// starts spilling into additional handle blocks.
const LOCAL_FRAME_CAPACITY: jint = 64;

thread_local! {
    /// Guards against the agent re-entering itself: a JNI call made from the
    /// callback can itself raise a Java exception, which fires another Exception
    /// event on the same thread. `const` init deliberately registers no TLS
    /// destructor — these are JVM-owned threads and the crate builds with
    /// `panic = "abort"`.
    static IN_CALLBACK: Cell<bool> = const { Cell::new(false) };
}

struct ReentryGuard;

impl ReentryGuard {
    fn enter() -> Option<ReentryGuard> {
        IN_CALLBACK.with(|flag| {
            if flag.get() {
                None
            } else {
                flag.set(true);
                Some(ReentryGuard)
            }
        })
    }
}

impl Drop for ReentryGuard {
    fn drop(&mut self) {
        IN_CALLBACK.with(|flag| flag.set(false));
    }
}

/// This is the Agent entry point that is called by the JVM during the loading phase.
/// Any failures in this function will cause the JVM not to start which is strictly
/// better than crashing later on. Therefore this function should return an error
/// code if continuing the loading process would put us in a bad state.
#[no_mangle]
#[allow(unused_variables)]
pub extern "C" fn Agent_OnLoad(
    vm: *mut JavaVM,
    options: *mut c_char,
    reserved: *mut c_void,
) -> jint {
    pretty_env_logger::init_custom_env("ROLLBAR_LOG");
    info!("Agent load begin");
    if let Err(e) = onload(vm) {
        return e;
    }
    info!("Agent load complete success");
    INIT_SUCCESS.store(true, Ordering::Relaxed);
    0
}

fn onload(vm: *mut JavaVM) -> Result<(), jint> {
    let mut jvmti_env = JvmTiEnv::new(vm)?;
    jvmti_env.enable_capabilities()?;

    // SetEventCallbacks replaces the whole table, so both callbacks are registered
    // here and delivery is toggled separately.
    let callbacks = jvmtiEventCallbacks {
        Exception: Some(c_on_exception),
        ClassPrepare: Some(c_on_class_prepare),
        ..Default::default()
    };
    jvmti_env.set_event_callbacks(&callbacks)?;

    // The Exception event is deliberately NOT enabled yet. Arming it at load time
    // means every exception thrown during VM and framework bootstrap enters the
    // callback -- tens of thousands of them on a Spring Boot startup -- before
    // ThrowableCache is even loadable. It is enabled from ClassPrepare instead.
    jvmti_env.set_event_mode(
        jvmtiEventMode_JVMTI_ENABLE,
        jvmtiEvent_JVMTI_EVENT_CLASS_PREPARE,
    )?;
    Ok(())
}

fn on_exception(jvmti_env: JvmTiEnv, mut jni_env: JniEnv, thread: jthread, exception: jobject) {
    let _guard = match ReentryGuard::enter() {
        Some(guard) => guard,
        None => return,
    };

    if jni_env.push_local_frame(LOCAL_FRAME_CAPACITY).is_err() {
        return;
    }
    let result = exceptions::inner_callback(jvmti_env, jni_env, thread, exception);
    // Never leave an exception pending across the frame pop or back into the
    // throwing thread.
    jni_env.clear_pending_exception();
    jni_env.pop_local_frame();

    if let Err(e) = result {
        debug!("{}", e);
    }
}

#[allow(unused_variables)]
unsafe extern "C" fn c_on_exception(
    jvmti_env: *mut jvmtiEnv,
    jni_env: *mut JNIEnv,
    thread: jthread,
    method: jmethodID,
    location: jlocation,
    exception: jobject,
    catch_method: jmethodID,
    catch_location: jlocation,
) -> () {
    if INIT_SUCCESS.load(Ordering::Relaxed) {
        let jvmti_env = JvmTiEnv::wrap(jvmti_env);
        on_exception(jvmti_env, JniEnv::new(jni_env), thread, exception);
    }
}

/// Fires for every class the VM prepares until the agent arms, so the miss path is
/// kept to a single `GetClassSignature` plus a byte comparison.
#[allow(unused_variables)]
unsafe extern "C" fn c_on_class_prepare(
    jvmti_env: *mut jvmtiEnv,
    jni_env: *mut JNIEnv,
    thread: jthread,
    klass: jclass,
) -> () {
    if !INIT_SUCCESS.load(Ordering::Relaxed) || cache::CORE.get().is_some() {
        return;
    }

    let mut jvmti = JvmTiEnv::wrap(jvmti_env);
    let expected = CStr::from_bytes_with_nul(b"Lcom/rollbar/jvmti/ThrowableCache;\0")
        .expect("signature literal is nul terminated");
    match jvmti.class_signature_is(klass, expected) {
        Ok(true) => {}
        _ => return,
    }

    let mut jni = JniEnv::new(jni_env);
    if let Err(e) = cache::arm(&mut jvmti, &mut jni, klass) {
        warn!("rollbar agent: could not initialise from ThrowableCache: {}", e);
        jni.clear_pending_exception();
        // Leave the Exception event disabled: an inert agent is the safe failure.
        return;
    }

    if let Err(e) = jvmti.set_event_mode(
        jvmtiEventMode_JVMTI_ENABLE,
        jvmtiEvent_JVMTI_EVENT_EXCEPTION,
    ) {
        warn!("rollbar agent: could not enable exception events: {}", e);
        return;
    }
    let _ = jvmti.set_event_mode(
        jvmtiEventMode_JVMTI_DISABLE,
        jvmtiEvent_JVMTI_EVENT_CLASS_PREPARE,
    );
    info!("rollbar agent: armed, exception capture enabled");
}
