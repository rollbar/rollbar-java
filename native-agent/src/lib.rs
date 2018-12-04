#![allow(non_upper_case_globals)]
#![allow(non_camel_case_types)]
#![allow(non_snake_case)]

extern crate pretty_env_logger;
#[macro_use]
extern crate log;
#[macro_use]
extern crate error_chain;

#[cfg_attr(feature = "cargo-clippy", allow(clippy))]
mod jvmti;
mod env;
mod errors;

use env::{JniEnv, JvmTiEnv};
use std::sync::atomic::{AtomicBool, Ordering, ATOMIC_BOOL_INIT};

static INIT_SUCCESS: AtomicBool = ATOMIC_BOOL_INIT;

#[no_mangle]
#[allow(unused_variables)]
pub extern "C" fn Agent_OnLoad(
    vm: *mut jvmti::JavaVM,
    options: *mut ::std::os::raw::c_char,
    reserved: *mut ::std::os::raw::c_void,
) -> jvmti::jint {
    pretty_env_logger::init_custom_env("ROLLBAR_LOG");
    info!("Agent load begin");
    if let Err(e) = onload(vm) {
        return e;
    }
    info!("Agent load complete success");
    INIT_SUCCESS.store(true, Ordering::Relaxed);
    0
}

fn onload(vm: *mut jvmti::JavaVM) -> Result<(), jvmti::jint> {
    let mut jvmti_env = JvmTiEnv::new(vm)?;
    jvmti_env.enable_capabilities()?;
    jvmti_env.set_exception_handler()?;
    Ok(())
}

fn on_exception(
    jvmti_env: JvmTiEnv,
    jni_env: JniEnv,
    thread: ::jvmti::jthread,
    exception: ::jvmti::jobject,
) {
    if let Err(e) = env::inner_callback(jvmti_env, jni_env, thread, exception) {
        debug!("{}", e);
    }
}

#[allow(unused_variables)]
unsafe extern "C" fn c_on_exception(
    jvmti_env: *mut ::jvmti::jvmtiEnv,
    jni_env: *mut ::jvmti::JNIEnv,
    thread: ::jvmti::jthread,
    method: ::jvmti::jmethodID,
    location: ::jvmti::jlocation,
    exception: ::jvmti::jobject,
    catch_method: ::jvmti::jmethodID,
    catch_location: ::jvmti::jlocation,
) -> () {
    if INIT_SUCCESS.load(Ordering::Relaxed) {
        let jvmti_env = JvmTiEnv::wrap(jvmti_env);
        on_exception(jvmti_env, JniEnv::new(jni_env), thread, exception);
    }
}
