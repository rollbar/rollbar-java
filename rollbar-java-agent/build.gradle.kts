plugins {
    `java-library`
    // Successor to the abandoned com.github.johnrengelman.shadow. Required at 9.x: Byte Buddy 1.18
    // ships Java 24 class files under META-INF/versions/24 (its bridge to the JDK's own class file
    // API), which older shadow releases cannot read. 9.5+ needs Gradle 9, so 9.4.3 is the ceiling
    // until this build's Gradle is upgraded.
    id("com.gradleup.shadow") version "9.4.3"
}

// Dependencies that get relocated into the fat jar, kept apart from the ones that must stay
// external. shadowJar merges runtimeClasspath by default, which would embed the Rollbar SDK and
// its transitive dependencies (SLF4J) unrelocated: the agent sits alongside the application's own
// Rollbar SDK, so duplicate com.rollbar.* classes can pin an older SDK version or — where the
// agent and the application resolve them from different classloaders — split class identity, so
// the TelemetryEvent the agent records is not the TelemetryEvent the SDK expects.
val shaded: Configuration by configurations.creating

// compileOnly: these are inside the jar, so they must not also be published as runtime
// dependencies of the agent.
configurations.compileOnly.configure { extendsFrom(shaded) }

dependencies {
    // Byte Buddy must be able to parse the class files of the JDK it runs on: the agent
    // instruments JDK classes, which always carry the running JDK's class file version. A version
    // older than the runtime fails to transform them (see the compatibility table at
    // https://github.com/raphw/byte-buddy#java-version-compatibility), so keep this current.
    shaded("net.bytebuddy:byte-buddy:1.18.11")
    shaded("net.bytebuddy:byte-buddy-agent:1.18.11")
    api(project(":rollbar-api"))
    implementation(project(":rollbar-java"))
    compileOnly("org.apache.httpcomponents:httpclient:4.5.14")
    compileOnly("org.apache.httpcomponents.client5:httpclient5:5.3.1")

    testImplementation(platform("org.junit:junit-bom:5.14.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.mockito:mockito-core:5.11.0")
    testImplementation("org.wiremock:wiremock:3.13.2")
    testImplementation("org.apache.httpcomponents:httpclient:4.5.14")
    testImplementation("org.apache.httpcomponents.client5:httpclient5:5.3.1")
}

tasks.jar {
    enabled = false
}

// java-library wires tasks.jar into apiElements/runtimeElements; replace it with shadowJar so
// Gradle's variant system and vanniktech publishing both see the fat jar as the primary artifact.
listOf(configurations.apiElements, configurations.runtimeElements).forEach { cfg ->
    cfg.configure {
        outgoing.artifacts.clear()
        outgoing.artifact(tasks.shadowJar)
    }
}

tasks.shadowJar {
    archiveClassifier.set("")
    // Embed only the `shaded` configuration, not the default runtimeClasspath. Everything else —
    // rollbar-api, rollbar-java, SLF4J — stays an ordinary external dependency resolved from the
    // application's own classpath.
    configurations.set(listOf(shaded))
    manifest {
        attributes(
            "Premain-Class" to "com.rollbar.agent.RollbarAgent",
            "Agent-Class" to "com.rollbar.agent.RollbarAgent",
            "Can-Redefine-Classes" to "true",
            "Can-Retransform-Classes" to "true"
        )
    }
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
