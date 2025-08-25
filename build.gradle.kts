plugins {
    alias(rollbarlibs.plugins.androidLibrary) apply false
    alias(rollbarlibs.plugins.vanniktech) apply false
    alias(rollbarlibs.plugins.spotbugs) apply false
    alias(rollbarlibs.plugins.revapi) apply false
    id("com.rollbar.conventions.release") apply false
}

val versionName = rootProject.extra["VERSION_NAME"] as String

allprojects {
    version = versionName

    repositories {
        google()
        mavenCentral()
    }
}

subprojects {
    val isExample = name.contains("examples") || parent?.name == "examples"
    val isAndroid = name.contains("android")

    if (isExample) {
        return@subprojects
    }

    apply(plugin = "com.rollbar.conventions.release")
    if (isAndroid) {
        return@subprojects
    }

    apply(plugin = "java-library")
    apply(from = "$rootDir/gradle/quality.gradle")
    apply(from = "$rootDir/gradle/compatibility.gradle")

    repositories {
        mavenCentral()
    }

    tasks.withType<Jar>().configureEach {
        manifest {
            attributes(
                mapOf(
                    "Implementation-Title" to project.name,
                    "Implementation-Version" to versionName
                )
            )
        }
    }

    dependencies {
        add("testImplementation", "junit:junit:4.13.2")
        add("testImplementation", "org.hamcrest:hamcrest-all:1.3")
        add("testImplementation", "org.mockito:mockito-core:5.18.0")
    }

    tasks.withType<JavaCompile>().configureEach {
        if (JavaVersion.current().isJava9Compatible) {
            options.release.set(8)
        } else {
            sourceCompatibility = JavaVersion.VERSION_1_8.toString()
            targetCompatibility = JavaVersion.VERSION_1_8.toString()
        }
    }
}