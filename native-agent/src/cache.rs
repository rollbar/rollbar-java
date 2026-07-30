//! Process-wide handle cache.
//!
//! Everything the Exception callback needs is resolved once and reused. Previously
//! each event re-ran `FindClass` and `GetStaticMethodID`, and each captured frame
//! re-resolved `CacheFrame`/`LocalVariable` plus a boxing class per primitive local.
//!
//! `jclass`/`jmethodID` are raw pointers and so not `Send`/`Sync` to Rust, but a JNI
//! *global* reference — and the method IDs of a class kept alive by one — are valid
//! on any thread for as long as the class stays loaded. The `unsafe impl`s below
//! encode exactly that. `OnceLock` supplies the acquire/release publication needed
//! for a callback on another thread to observe a fully initialised struct.

use std::sync::OnceLock;

use env::JvmTiEnv;
use errors::*;
use jni::JniEnv;
use jvmti::{jclass, jfieldID, jmethodID, jobject};

pub struct BoxType {
    pub class: jclass,
    pub value_of: jmethodID,
}

/// Resolved when `com.rollbar.jvmti.ThrowableCache` is prepared. Its presence is
/// what arms the Exception event.
pub struct Core {
    pub throwable_cache: jclass,
    pub should_cache: jmethodID,
    pub add: jmethodID,
    /// Defining loader of `ThrowableCache`; sibling classes resolve through it.
    pub loader: jobject,
    /// `ThrowableCache.appPackagesVersion`, absent on older rollbar-java jars.
    pub app_packages_version: Option<jfieldID>,
    /// `ThrowableCache.appPackagesSnapshot()`, absent on older rollbar-java jars.
    pub app_packages_snapshot: Option<jmethodID>,
}

unsafe impl Send for Core {}
unsafe impl Sync for Core {}

/// Resolved lazily on the first exception that actually gets captured, so apps that
/// never capture anything never pay for it.
pub struct Capture {
    pub cache_frame: jclass,
    pub cache_frame_ctor: jmethodID,
    pub local_variable: jclass,
    pub local_variable_ctor: jmethodID,
    pub long_box: BoxType,
    pub float_box: BoxType,
    pub double_box: BoxType,
    pub int_box: BoxType,
    pub short_box: BoxType,
    pub char_box: BoxType,
    pub byte_box: BoxType,
    pub bool_box: BoxType,
}

unsafe impl Send for Capture {}
unsafe impl Sync for Capture {}

pub static CORE: OnceLock<Core> = OnceLock::new();
pub static CAPTURE: OnceLock<Capture> = OnceLock::new();

/// Promotes the `ClassPrepare` class to a global ref and resolves the two static
/// methods on it. The supplied `jclass` is the real class from whichever loader
/// loaded it, which is why this works where `FindClass` does not.
pub fn arm(jvmti: &mut JvmTiEnv, jni: &mut JniEnv, klass: jclass) -> Result<()> {
    let throwable_cache = jni.new_global_ref(klass)? as jclass;

    let should_cache = jni.get_static_method_id(
        throwable_cache,
        "shouldCacheThrowable",
        "(Ljava/lang/Throwable;I)Z",
    )?;
    let add = jni.get_static_method_id(
        throwable_cache,
        "add",
        "(Ljava/lang/Throwable;[Lcom/rollbar/jvmti/CacheFrame;)V",
    )?;

    let loader_local = jvmti.get_class_loader(throwable_cache)?;
    let loader = jni.new_global_ref(loader_local)?;

    // Both are absent on rollbar-java jars older than the version that added the
    // native pre-filter; the agent then falls back to filtering in Java only.
    let app_packages_version =
        jni.get_static_field_id_opt(throwable_cache, "appPackagesVersion", "I");
    let app_packages_snapshot = jni.get_static_method_id_opt(
        throwable_cache,
        "appPackagesSnapshot",
        "()[Ljava/lang/String;",
    );
    if app_packages_version.is_none() || app_packages_snapshot.is_none() {
        info!(
            "rollbar agent: rollbar-java predates the native package filter; \
             falling back to filtering in Java"
        );
    }

    let core = Core {
        throwable_cache,
        should_cache,
        add,
        loader,
        app_packages_version,
        app_packages_snapshot,
    };

    if let Err(duplicate) = CORE.set(core) {
        // Another ClassPrepare won the race; release the refs we just took.
        jni.delete_global_ref(duplicate.throwable_cache);
        jni.delete_global_ref(duplicate.loader);
    }
    Ok(())
}

pub fn capture(jni: &mut JniEnv, core: &Core) -> Result<&'static Capture> {
    if let Some(existing) = CAPTURE.get() {
        return Ok(existing);
    }

    let built = build_capture(jni, core)?;
    if let Err(duplicate) = CAPTURE.set(built) {
        release_capture(jni, &duplicate);
    }
    Ok(CAPTURE.get().expect("CAPTURE set above"))
}

fn build_capture(jni: &mut JniEnv, core: &Core) -> Result<Capture> {
    let class_class = jni.find_class("java/lang/Class")?;
    let for_name = jni.get_static_method_id(
        class_class,
        "forName",
        "(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;",
    )?;

    let cache_frame_local = jni.class_for_name(
        class_class,
        for_name,
        "com.rollbar.jvmti.CacheFrame",
        core.loader,
    )?;
    let cache_frame = jni.new_global_ref(cache_frame_local)? as jclass;
    let cache_frame_ctor = jni.get_method_id(
        cache_frame,
        "<init>",
        "(Ljava/lang/reflect/Method;[Lcom/rollbar/jvmti/LocalVariable;)V",
    )?;

    let local_variable_local = jni.class_for_name(
        class_class,
        for_name,
        "com.rollbar.jvmti.LocalVariable",
        core.loader,
    )?;
    let local_variable = jni.new_global_ref(local_variable_local)? as jclass;
    let local_variable_ctor = jni.get_method_id(
        local_variable,
        "<init>",
        "(Ljava/lang/String;Ljava/lang/Object;)V",
    )?;

    Ok(Capture {
        cache_frame,
        cache_frame_ctor,
        local_variable,
        local_variable_ctor,
        long_box: box_type(jni, "java/lang/Long", "(J)Ljava/lang/Long;")?,
        float_box: box_type(jni, "java/lang/Float", "(F)Ljava/lang/Float;")?,
        double_box: box_type(jni, "java/lang/Double", "(D)Ljava/lang/Double;")?,
        int_box: box_type(jni, "java/lang/Integer", "(I)Ljava/lang/Integer;")?,
        short_box: box_type(jni, "java/lang/Short", "(S)Ljava/lang/Short;")?,
        char_box: box_type(jni, "java/lang/Character", "(C)Ljava/lang/Character;")?,
        byte_box: box_type(jni, "java/lang/Byte", "(B)Ljava/lang/Byte;")?,
        bool_box: box_type(jni, "java/lang/Boolean", "(Z)Ljava/lang/Boolean;")?,
    })
}

/// `java.lang.*` wrappers live in the bootstrap loader, so `FindClass` is safe here.
fn box_type(jni: &mut JniEnv, name: &str, signature: &str) -> Result<BoxType> {
    let local = jni.find_class(name)?;
    let class = jni.new_global_ref(local)? as jclass;
    let value_of = jni.get_static_method_id(class, "valueOf", signature)?;
    Ok(BoxType { class, value_of })
}

fn release_capture(jni: &mut JniEnv, capture: &Capture) {
    jni.delete_global_ref(capture.cache_frame);
    jni.delete_global_ref(capture.local_variable);
    for boxed in &[
        &capture.long_box,
        &capture.float_box,
        &capture.double_box,
        &capture.int_box,
        &capture.short_box,
        &capture.char_box,
        &capture.byte_box,
        &capture.bool_box,
    ] {
        jni.delete_global_ref(boxed.class);
    }
}
