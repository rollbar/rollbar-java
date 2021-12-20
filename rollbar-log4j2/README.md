# Rollbar Log4j 2 integration

This directory contains the Log4j 2 integration of the Rollbar Java SDK.

Instructions for building and contributing to the SDK can be found in the main repository [README](../README.md).

## Compatibility

Staring with version `1.8.0`, `rollbar-log4j2` depends on version `2.16.0` (or later) of `log4j-core`.
This removes compatibility with Java 7, but was a necessary upgrade to fix the following vulnerabilites in Log4j:

- CVE-2021-44228
- CVE-2021-45046
- CVE-2021-45105

Projects built and/or running with Java 7 can still use `rollbar-log4j2` version `1.8.0+`,
while forcing the use of a **vulnerable**, Java 7 compatible version of `Log4j`,
by updating their build configuration to ignore transitive dependencies from `rollbar-log4j2`.

Gradle configuration:

```gradle
dependencies {
    implementation(group: 'com.rollbar', name: 'rollbar-log4j2', version: '1.8.1') {
        exclude group: 'org.apache.logging.log4j'
    }

    implementation group: 'org.apache.logging.log4j', name: 'log4j-slf4j-impl', version: '2.12.2'
    annotationProcessor group: 'org.apache.logging.log4j', name: 'log4j-core', version: '2.12.2'
}

```

While CVE-2021-44228 and CVE-2021-45046 are already fixed in `2.12.2`, CVE-2021-45105 is **not** fixed for Java 7.
Note CVE-2021-45105 is a high DoS vulnerability and this approach should only be used after a thorough security analysis, and with very strong mitigations in place.

