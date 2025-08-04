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

if (isJava8) {
    println("Java 8 detected: excluding :rollbar-android and :examples:rollbar-android")
} else {
    println("Java ${JavaVersion.current()} detected: including Android modules")
    include(":rollbar-android", ":examples:rollbar-android")
}
