group = "com.rollbar.okhttp"

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.14.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("com.squareup.okhttp3:mockwebserver:5.3.2")
    testImplementation("org.mockito:mockito-core:5.23.0")
    implementation("com.squareup.okhttp3:okhttp:5.3.2")
    api(project(":rollbar-api"))
    api("org.slf4j:slf4j-api:1.7.25")
}

tasks.test {
    useJUnitPlatform()
}
