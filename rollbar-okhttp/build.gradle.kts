plugins {
    id("java")
}

group = "com.rollbar.okhttp"
version = "2.2.0"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.9.2")
    testImplementation("org.mockito:mockito-core:5.23.0")
    implementation("com.squareup.okhttp3:okhttp:4.9.2")
    api(project(":rollbar-api"))
}

tasks.test {
    useJUnitPlatform()
}
