plugins {
    `java-library`
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

dependencies {
    implementation("net.bytebuddy:byte-buddy:1.14.18")
    implementation("net.bytebuddy:byte-buddy-agent:1.14.18")
    api(project(":rollbar-api"))
    implementation(project(":rollbar-java"))
    compileOnly("org.apache.httpcomponents:httpclient:4.5.14")
    compileOnly("org.apache.httpcomponents.client5:httpclient5:5.3.1")

    testImplementation(platform("org.junit:junit-bom:5.14.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.mockito:mockito-core:5.11.0")
    testImplementation("com.github.tomakehurst:wiremock-jre8:2.35.2")
    testImplementation("org.apache.httpcomponents:httpclient:4.5.14")
    testImplementation("org.apache.httpcomponents.client5:httpclient5:5.3.1")
}

tasks.jar {
    manifest {
        attributes(
            "Premain-Class" to "com.rollbar.agent.RollbarAgent",
            "Agent-Class" to "com.rollbar.agent.RollbarAgent",
            "Can-Redefine-Classes" to "true",
            "Can-Retransform-Classes" to "true"
        )
    }
}

tasks.shadowJar {
    archiveClassifier.set("")
    relocate("net.bytebuddy", "com.rollbar.agent.shaded.bytebuddy")
    mergeServiceFiles()
}

// Override root's Java 8 compatibility — this agent targets Java 11+ to support
// java.net.http.HttpClient instrumentation.
tasks.withType<JavaCompile>().configureEach {
    options.release.set(11)
}

tasks.test {
    useJUnitPlatform()
    val agentJar = tasks.shadowJar.get().archiveFile.get().asFile
    // Load as Java agent (instruments HTTP classes on startup)
    jvmArgs("-javaagent:$agentJar")
    // Also put on test classpath — the TCCL reflection bridge finds agent classes via the
    // system classloader; mirrors production use where rollbar-java-agent is a Gradle/Maven dep
    classpath += files(agentJar)
    dependsOn(tasks.shadowJar)
}
