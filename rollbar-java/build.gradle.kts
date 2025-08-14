import java.io.File

plugins {
    id("com.netflix.nebula.integtest") version "10.0.1"
}

val integTestArtifacts by configurations.creating
val integTestRuntime by configurations.creating {
    extendsFrom(configurations["integTestImplementation"])
    isCanBeConsumed = true
    isCanBeResolved = true
}

dependencies {
    api(project(":rollbar-api"))
    api("org.slf4j:slf4j-api:1.7.25")

    compileOnly("com.google.code.findbugs:jsr305:3.0.2")

    testImplementation("com.google.code.gson:gson:2.8.6")

    "integTestImplementation"("com.github.tomakehurst:wiremock:2.27.0")
    "integTestImplementation"("com.google.code.gson:gson:2.8.2")
}

val versionName = project.version.toString().ifEmpty { "unspecified" }

/**
 * This task will create a version property that is statically referenced when populating the
 * `notifier` section of the payload. It helps when users shade and / or relocate the
 * `rollbar-java` classes, since in those cases we no longer have access to our jar manifest.
 * The task creates a Java class instead of a text resource, since dynamically loaded resources
 * are not as reliable under relocation as a strongly typed bytecode reference to a compiled class.
 */
val createVersionClass by tasks.registering {
    val outputDir = layout.buildDirectory.dir("src/generated/main")
    outputs.dir(outputDir)

    doLast {
        val pkg = listOf("com", "rollbar", "notifier", "provider", "notifier")
        val pkgName = pkg.joinToString(".")
        val pkgPath = outputDir.get().asFile.resolve(pkg.joinToString(File.separator))
        val escapedVersion = versionName.replace("\\", "\\\\").replace("\"", "\\\"")

        val classText = """
            package $pkgName;

            class VersionHelperResources {
              static String getVersion() {
                return "$escapedVersion";
              }
            }
        """.trimIndent()

        pkgPath.mkdirs()
        pkgPath.resolve("VersionHelperResources.java").writeText(classText)
    }
}

sourceSets {
    named("main") {
        java.srcDir(createVersionClass.map { it.outputs.files.singleFile })
    }
}

tasks.named("compileJava") {
    dependsOn(createVersionClass)
}

tasks.named("checkstyleMain") {
    dependsOn(createVersionClass)
}

//Ensure sourcesJar runs after version class is created
tasks.withType<Jar>().configureEach {
    if (name == "sourcesJar") {
        dependsOn(createVersionClass)
    }
}

tasks.test {
    // This helps us test the VersionHelper class since there's no jar manifest available when
    // running tests.
    systemProperty("ROLLBAR_IMPLEMENTATION_VERSION", versionName)
}

// The 'java-test-fixtures' plugin is not getting along with 'nebula.integtest', so we'll use this instead
val integTestJar by tasks.registering(Jar::class) {
    archiveClassifier.set("integtest")
    from(sourceSets["integTest"].output)
    dependsOn(tasks.named("integTestClasses"))
}

artifacts {
    add("integTestArtifacts", integTestJar)
}
