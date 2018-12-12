extern crate bindgen;

use std::env;
use std::path::PathBuf;

fn main() {
    let bindings = bindgen::Builder::default()
        .derive_default(true)
        .header(jvmti_wrapper())
        .clang_arg(clang_include(&java_include()))
        .clang_arg(clang_include(&java_include_platform()))
        .generate()
        .expect("Failed to generate bindings");

    bindings
        .write_to_file(jvmti_bindings())
        .expect("Failed to write bindings");
}

fn clang_include(path: &PathBuf) -> String {
    format!("-I/{}", path.to_str().unwrap())
}

fn java_include() -> PathBuf {
    return PathBuf::from(env::var("JAVA_HOME").unwrap()).join("include");
}

fn java_include_platform() -> PathBuf {
    if cfg!(target_os = "macos") {
        return java_include().join("darwin");
    } else {
        return java_include().join("linux");
    }
}

fn jvmti_bindings() -> PathBuf {
    return PathBuf::from(env::var("OUT_DIR").unwrap()).join("jvmti_bindings.rs");
}

fn jvmti_wrapper() -> String {
    return String::from("include/jvmti_wrapper.h");
}
