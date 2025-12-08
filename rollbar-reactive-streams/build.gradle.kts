plugins {
    id("com.netflix.nebula.integtest") version "10.0.1"
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    api(project(":rollbar-java"))
    api("org.reactivestreams:reactive-streams:1.0.3")
    compileOnly("org.apache.httpcomponents.client5:httpclient5:5.5.1")

    testImplementation("org.reactivestreams:reactive-streams-tck:1.0.4")
    testImplementation("org.mockito:mockito-core:5.13.0")
    testImplementation("org.testng:testng:7.10.2")
    testImplementation("org.apache.httpcomponents.client5:httpclient5:5.5.1")
    testImplementation("org.apache.commons:commons-lang3:3.12.0")
    testImplementation("com.google.code.gson:gson:2.13.2")

    testImplementation(platform("io.projectreactor:reactor-bom:2020.0.6"))
    testImplementation("io.projectreactor:reactor-core")

    // Reuse some of the tests since we're providing compatible implementations
    testImplementation(project(path = ":rollbar-java", configuration = "integTestRuntime"))
    testImplementation(project(path = ":rollbar-java", configuration = "integTestArtifacts"))

    integTestImplementation("com.github.tomakehurst:wiremock-jre8:2.27.2")
    integTestImplementation("com.google.code.gson:gson:2.13.2")
}

// The reactive streams TCK tests use TestNG
tasks.register<Test>("tckTest") {
    useTestNG()
}

tasks.withType<JavaCompile>().configureEach {
    // For main sources: keep targeting Java 8
    if (name.contains("main", ignoreCase = true)) {
        options.release.set(8)
    }
    // For test sources: bump to Java 11
    if (name.contains("test", ignoreCase = true)) {
        options.release.set(11)
    }
}

tasks.named("check") {
    dependsOn("tckTest")
}

tasks.named("integrationTest") {
    dependsOn(project(":rollbar-java").tasks.named("integTestJar"))
}
