plugins {
    `kotlin-dsl`
}

group = "com.rollbar.buildlogic"

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation("com.vanniktech:gradle-maven-publish-plugin:0.34.0")
}

gradlePlugin {
    plugins {
        create("rollbar-release-plugin") {
            id = "com.rollbar.conventions.release"
            implementationClass = "RollbarPublishPlugin"
        }
    }
}
