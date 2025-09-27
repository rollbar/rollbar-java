import com.github.spotbugs.snom.Confidence
import com.github.spotbugs.snom.Effort
import com.github.spotbugs.snom.SpotBugsTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.quality.Checkstyle
import org.gradle.api.plugins.quality.CheckstyleExtension
import org.gradle.testing.jacoco.tasks.JacocoReport

class AndroidQualityPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        with(project) {

            pluginManager.apply("checkstyle")
            pluginManager.apply("com.github.spotbugs-base")
            pluginManager.apply("jacoco")

            extensions.configure(CheckstyleExtension::class.java) {
                configFile = rootProject.file("tools/checkstyle/google_checks.xml")
            }

            tasks.register("checkstyleMain", Checkstyle::class.java) {
                source("src")
                include("**/*.java")
                exclude("**/gen/**", "**/*Test.java", "**/annotation/*.java")
                classpath = files()
                reports {
                    xml.required.set(false)
                    html.required.set(true)
                }
                isIgnoreFailures = true
            }

            extensions.configure<com.github.spotbugs.snom.SpotBugsExtension>("spotbugs") {
                includeFilter.set(rootProject.file("tools/findbugs/findbugs.xml"))
            }

            tasks.register("spotbugsMain", SpotBugsTask::class.java) {
                dependsOn(tasks.matching { it.name.startsWith("assemble") })

                ignoreFailures = true
                effort.set(Effort.MAX)
                reportLevel.set(Confidence.MEDIUM)

                classes = fileTree("build/intermediates/javac/debug/classes/")
                sourceDirs.from(fileTree("src/main/java"))
                auxClassPaths.from(files())

                projectName.set(project.name)
                release.set(project.version.toString())

                reports.create("html") {
                    required.set(true)
                    outputLocation.set(layout.buildDirectory.file("reports/spotbugs/main/spotbugs.html").get().asFile)
                    setStylesheet("fancy-hist.xsl")
                }
            }

            extensions.configure<org.gradle.testing.jacoco.plugins.JacocoPluginExtension>("jacoco") {
                toolVersion = "0.8.6"
            }

            tasks.register("jacocoTestReport", JacocoReport::class.java) {
                dependsOn("test")

                reports {
                    xml.required.set(false)
                    csv.required.set(false)
                    html.required.set(true)
                    html.outputLocation.set(layout.buildDirectory.dir("jacocoHtml"))
                }
            }

            tasks.named("check") {
                dependsOn("checkstyleMain", "spotbugsMain")
            }
        }
    }
}
