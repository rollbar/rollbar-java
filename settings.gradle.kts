rootProject.name="rollbar-java-sdk"


pluginManagement {
    includeBuild("build-logic")

    plugins {
        id("com.android.library") version "8.6.0"
    }

    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }

    versionCatalogs {
        create("rollbarlibs") {
            from(files("gradle/rollbarlibs.versions.toml"))
        }
    }
}


include(
    ":rollbar-api",
    ":rollbar-java",
    ":rollbar-web",
    ":rollbar-jakarta-web",
    ":rollbar-log4j2",
    ":rollbar-logback",
    ":rollbar-okhttp",
    ":rollbar-spring-webmvc",
    ":rollbar-spring6-webmvc",
    ":rollbar-spring-boot-webmvc",
    ":rollbar-spring-boot3-webmvc",
    ":rollbar-struts2",
    ":rollbar-reactive-streams",
    ":rollbar-reactive-streams-reactor",
    ":examples:rollbar-java",
    ":examples:rollbar-web",
    ":examples:rollbar-scala",
    ":examples:rollbar-log4j2",
    ":examples:rollbar-logback",
    ":examples:rollbar-spring-webmvc",
    ":examples:rollbar-spring-boot-webmvc",
    ":examples:rollbar-struts2",
    ":examples:rollbar-struts2-spring",
    ":examples:rollbar-reactive-streams-reactor"
)

val isJava8 = JavaVersion.current() == JavaVersion.VERSION_1_8
val isJava11 = JavaVersion.current() == JavaVersion.VERSION_11

if (isJava8 || isJava11) {
    println("Java 8 or 11 detected: excluding :rollbar-android and :examples:rollbar-android")
} else {
    println("Java ${JavaVersion.current()} detected: including Android modules")
    include(":rollbar-android", ":examples:rollbar-android")
}

// The agent's own artifact targets Java 11+ (JavaCompile release = 11), but *building* it needs a
// Java 17+ JVM: it is packaged by com.gradleup.shadow 9.x, whose plugin marker declares a JVM 17
// runtime requirement, so on an older JVM the build fails while resolving the plugin classpath —
// before any task runs. Shadow 9.x is not optional here (Byte Buddy 1.18 ships Java 24 class files
// under META-INF/versions/24, which earlier shadow releases cannot read), and a Java toolchain does
// not help because the plugin is resolved against the Gradle daemon's JVM, not the toolchain.
//
// So exclude the module on Java 8 and 11. The CI matrix still builds and tests it on 17, and the
// release job runs on 17.
if (JavaVersion.current() < JavaVersion.VERSION_17) {
    println("Java ${JavaVersion.current()} detected: excluding :rollbar-java-agent (building it requires Java 17+)")
} else {
    include(":rollbar-java-agent")
}
