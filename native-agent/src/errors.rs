error_chain! {
    errors {
        Jni(s: String) {
            description("JNI call failure")
            display("JNI call failed: '{}'", s)
        }

        JvmTi(s: String, rc: ::jvmti::jint) {
            description("JVMTI call failure")
            display("{}: {:?}", s, rc)
        }

        Internal(s: String) {
            description("internal error")
            display("internal error: '{}'", s)
        }
    }
}

use std::ffi::NulError;

impl From<NulError> for Error {
    fn from(_e: NulError) -> Error {
        ErrorKind::Internal("Unexpected interior nul byte".into()).into()
    }
}

impl From<Error> for ::jvmti::jint {
    fn from(err: Error) -> ::jvmti::jint {
        match err {
            Error(ErrorKind::JvmTi(_, rc), _) => rc,
            Error(ErrorKind::Internal(_), _) => 113, // JVMTI_ERROR_INTERNAL
            _ => 0,
        }
    }
}
