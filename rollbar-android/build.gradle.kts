plugins {
    id("com.android.library")
    id("com.rollbar.conventions.release")
}

//apply(from = "$rootDir/gradle/android.quality.gradle") //TODO: Update as convention plugin

android {
    namespace = "com.rollbar.android"
    compileSdk = 33

    defaultConfig {
        minSdk = 21
        targetSdk = 33
        consumerProguardFiles("proguard-rules.pro")
        manifestPlaceholders["notifierVersion"] = project.property("VERSION_NAME") as String
    }

    buildTypes {
        getByName("release") {
            enableUnitTestCoverage = false
            enableAndroidTestCoverage = false
        }
        getByName("debug") {
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    api(project(":rollbar-java"))

    testImplementation("junit:junit:4.13.1")
    testImplementation("org.hamcrest:hamcrest-all:1.3")
    testImplementation("org.mockito:mockito-core:5.18.0")
    androidTestImplementation("org.mockito:mockito-android:5.18.0")
}