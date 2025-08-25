import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

class RollbarPublishPlugin : Plugin<Project> {
    override fun apply(project: Project) {

        project.pluginManager.withPlugin("com.android.library") {
            applyPlugin(project)
        }

        project.plugins.withId("java-library") {
            applyPlugin(project)
        }

    }

    private fun applyPlugin(project: Project) {
        project.plugins.apply("com.vanniktech.maven.publish")

        project.extensions.configure(MavenPublishBaseExtension::class.java) {
            coordinates(
                groupId = project.findProperty("GROUP") as String,
                artifactId = project.name,
                version = project.findProperty("VERSION_NAME") as String,
            )

            pom {
                name.set(project.findProperty("POM_NAME") as String)
                description.set(project.findProperty("POM_DESCRIPTION") as String)
                url.set(project.findProperty("POM_URL") as String)

                licenses {
                    license {
                        name.set(project.findProperty("POM_LICENCE_NAME") as String)
                        url.set(project.findProperty("POM_LICENCE_URL") as String)
                        distribution.set(project.findProperty("POM_LICENCE_DIST") as String)
                    }
                }

                developers {
                    developer {
                        id.set("rokob")
                        name.set("Andrew Weiss")
                    }
                    developer {
                        id.set("basoko")
                        name.set("David Basoco")
                    }
                    developer {
                        id.set("diegov")
                        name.set("Diego Veralli")
                    }
                }

                scm {
                    url.set(project.findProperty("POM_SCM_URL") as String)
                    connection.set(project.findProperty("POM_SCM_CONNECTION") as String)
                    developerConnection.set(project.findProperty("POM_SCM_DEV_CONNECTION") as String)
                }
            }

            publishToMavenCentral()
        }
    }
}
