//! Native app-package pre-filter.
//!
//! `ThrowableCache.shouldCacheThrowable` calls `throwable.getStackTrace()`, which
//! decodes the backtrace and allocates a `StackTraceElement[]` for *every* exception
//! in the JVM once app packages are configured. On a framework-heavy startup the vast
//! majority of those are rejected, so paying for the trace is wasted.
//!
//! This module answers the same question natively, from the JVMTI stack, memoizing the
//! per-method decision so repeated throws from the same code cost a hash lookup.
//!
//! It is only ever used to make the *reject* path cheap. Anything that passes still
//! goes through `shouldCacheThrowable`, which remains the authority and also performs
//! the per-throwable dedup.

use std::collections::HashMap;
use std::sync::atomic::{AtomicI32, Ordering};
use std::sync::{OnceLock, RwLock};

use cache::Core;
use env::JvmTiEnv;
use errors::*;
use jni::JniEnv;
use jvmti::{jint, jmethodID, jthread, jvmtiFrameInfo};

/// Frames examined when deciding whether a throw is app-related. The Java filter
/// scans the whole trace; this bound only applies to the fast path, and anything not
/// rejected here still gets the full Java check.
const MAX_SCAN_FRAMES: jint = 128;

/// Cap on memoized method decisions. `jmethodID`s are invalidated by class unloading
/// and can be recycled, so the map is cleared wholesale rather than grown forever.
/// A stale entry can only mis-classify one frame; the id is compared as an integer
/// and never dereferenced.
const MAX_DECISIONS: usize = 100_000;

static KNOWN_VERSION: AtomicI32 = AtomicI32::new(-1);

/// Package prefixes in internal form (`com/foo`), matching JVMTI class signatures.
static PREFIXES: RwLock<Vec<Vec<u8>>> = RwLock::new(Vec::new());

static DECISIONS: OnceLock<RwLock<HashMap<usize, bool>>> = OnceLock::new();

fn decisions() -> &'static RwLock<HashMap<usize, bool>> {
    DECISIONS.get_or_init(|| RwLock::new(HashMap::new()))
}

/// Outcome of the cheap pre-check.
pub enum Decision {
    /// No app packages configured: `shouldCacheThrowable` could only return false.
    Skip,
    /// No app frame on the stack; reject without materializing a Java stack trace.
    NoAppFrame,
    /// Either an app frame was found, or the filter is unavailable. Ask Java.
    AskJava,
}

/// Reads the version counter and, when it has changed, refreshes the native prefix
/// list. `GetStaticIntField` is a plain memory read through the JNI table: no
/// allocation, no Java execution.
pub fn decide(
    jvmti_env: &mut JvmTiEnv,
    jni_env: &mut JniEnv,
    core: &Core,
    thread: jthread,
) -> Decision {
    let (version_field, snapshot_method) = match (core.app_packages_version, core.app_packages_snapshot)
    {
        (Some(field), Some(method)) => (field, method),
        // Older rollbar-java: no cheap gate available, defer to Java entirely.
        _ => return Decision::AskJava,
    };

    let version = jni_env.get_static_int_field(core.throwable_cache, version_field);
    if version == 0 {
        return Decision::Skip;
    }

    if version != KNOWN_VERSION.load(Ordering::Relaxed) {
        if refresh(jni_env, core, snapshot_method).is_err() {
            jni_env.clear_pending_exception();
            return Decision::AskJava;
        }
        KNOWN_VERSION.store(version, Ordering::Relaxed);
    }

    match stack_has_app_frame(jvmti_env, thread) {
        Ok(true) => Decision::AskJava,
        Ok(false) => Decision::NoAppFrame,
        // Never fail closed: if the native scan breaks, let Java decide.
        Err(_) => Decision::AskJava,
    }
}

fn refresh(jni_env: &mut JniEnv, core: &Core, snapshot_method: jmethodID) -> Result<()> {
    let array = jni_env.call_static_object_method0(core.throwable_cache, snapshot_method)?;
    let length = jni_env.get_array_length(array);

    let mut prefixes = Vec::with_capacity(length as usize);
    for i in 0..length {
        let element = jni_env.get_object_array_element(array, i);
        if let Some(bytes) = jni_env.string_to_bytes(element) {
            // Java compares against dotted class names; JVMTI signatures use slashes.
            prefixes.push(
                bytes
                    .into_iter()
                    .map(|b| if b == b'.' { b'/' } else { b })
                    .collect(),
            );
        }
    }

    if let Ok(mut guard) = PREFIXES.write() {
        *guard = prefixes;
    }
    // Prefixes changed, so cached per-method verdicts are no longer valid.
    if let Ok(mut guard) = decisions().write() {
        guard.clear();
    }
    Ok(())
}

fn stack_has_app_frame(jvmti_env: &mut JvmTiEnv, thread: jthread) -> Result<bool> {
    let mut frames = [jvmtiFrameInfo::default(); MAX_SCAN_FRAMES as usize];
    let mut returned: jint = 0;
    jvmti_env.get_stack_trace(
        thread,
        0,
        MAX_SCAN_FRAMES,
        frames.as_mut_ptr(),
        &mut returned,
    )?;

    let returned = returned.max(0).min(MAX_SCAN_FRAMES) as usize;
    for frame in frames.iter().take(returned) {
        if is_app_method(jvmti_env, frame.method)? {
            return Ok(true);
        }
    }
    Ok(false)
}

/// Whether a frame belongs to app code, for deciding if its locals are worth
/// collecting. When no prefixes are known -- an older rollbar-java, or packages not
/// yet configured -- every frame counts as app code so behaviour matches the
/// pre-filter agent rather than silently capturing nothing.
pub fn is_app_frame(jvmti_env: &mut JvmTiEnv, method: jmethodID) -> bool {
    match PREFIXES.read() {
        Ok(prefixes) if prefixes.is_empty() => return true,
        Err(_) => return true,
        Ok(_) => {}
    }
    is_app_method(jvmti_env, method).unwrap_or(true)
}

fn is_app_method(jvmti_env: &mut JvmTiEnv, method: jmethodID) -> Result<bool> {
    let key = method as usize;
    if let Ok(guard) = decisions().read() {
        if let Some(&known) = guard.get(&key) {
            return Ok(known);
        }
    }

    let mut declaring = ::std::ptr::null_mut();
    jvmti_env.get_method_declaring_class(method, &mut declaring)?;
    let signature = jvmti_env.get_class_signature(declaring)?;

    // "Lcom/foo/Bar;" -> "com/foo/Bar". Anything else (arrays, primitives) is not app code.
    let internal_name = match signature.split_first() {
        Some((&b'L', rest)) => rest,
        _ => &[][..],
    };

    let matched = match PREFIXES.read() {
        Ok(prefixes) => prefixes
            .iter()
            .any(|prefix| internal_name.starts_with(prefix.as_slice())),
        Err(_) => false,
    };

    if let Ok(mut guard) = decisions().write() {
        if guard.len() >= MAX_DECISIONS {
            guard.clear();
        }
        guard.insert(key, matched);
    }
    Ok(matched)
}
