#![allow(non_upper_case_globals)]
#![allow(non_camel_case_types)]
#![allow(non_snake_case)]

extern crate pretty_env_logger;
#[macro_use]
extern crate log;
#[macro_use]
extern crate error_chain;

mod env;
mod errors;
mod exceptions;
mod jni;

#[cfg_attr(feature = "cargo-clippy", allow(clippy))]
mod jvmti;

use env::JvmTiEnv;
use jni::JniEnv;
use jvmti::{jint, jlocation, jmethodID, jobject, jthread, jvmtiEnv, JNIEnv, JavaVM};
use std::os::raw::{c_char, c_void};
use std::sync::atomic::{AtomicBool, Ordering, ATOMIC_BOOL_INIT};

static INIT_SUCCESS: AtomicBool = ATOMIC_BOOL_INIT;

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
    jvmti_env.set_exception_handler()?;
    Ok(())
}

fn on_exception(jvmti_env: JvmTiEnv, jni_env: JniEnv, thread: jthread, exception: jobject) {
    if let Err(e) = exceptions::inner_callback(jvmti_env, jni_env, thread, exception) {
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
