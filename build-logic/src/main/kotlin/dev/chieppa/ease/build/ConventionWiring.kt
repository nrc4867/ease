package dev.chieppa.ease.build

import dev.chieppa.ease.build.sources.CheckSources
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.kotlin.dsl.withType

/** Wiring the engine's and the games' convention plugins share. */
private const val STAGED_FILES_PROPERTY = "stagedFiles"

/** The dependencies and settings every module's JUnit tests run with. */
fun Project.configureJunitTests() {
  dependencies.add("testImplementation", dependencies.platform(catalogLibrary("junit-bom")))
  dependencies.add("testImplementation", catalogLibrary("junit-jupiter"))
  dependencies.add("testRuntimeOnly", catalogLibrary("junit-platform-launcher"))
  tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    jvmArgs("--enable-native-access=ALL-UNNAMED")
    testLogging.exceptionFormat = TestExceptionFormat.FULL
  }
}

/**
 * Keeps detekt off the code a build writes under `build/`, and makes `check` run it per source set:
 * rules such as the forbidden annotations need type resolution, which only those tasks have.
 */
fun Project.configureDetektChecks() {
  tasks.withType<dev.detekt.gradle.Detekt>().configureEach {
    val generated = layout.buildDirectory.get().asFile
    setSource(source.filter { !it.startsWith(generated) })
  }
  tasks.named("check") { dependsOn("detektMain", "detektTest") }
}

/**
 * Registers `checkSources` over this project's code and Markdown, minus [excludes] (globs under the
 * project), and makes `check` run it. The pre-commit hook restricts it to the staged files through
 * `-PstagedFiles`.
 */
fun Project.registerCheckSources(excludes: List<String> = emptyList()): TaskProvider<CheckSources> {
  val checkSources =
    tasks.register("checkSources", CheckSources::class.java) {
      group = "verification"
      description = "Checks every source and Markdown file against the rules no linter covers."
      sources.from(
        fileTree(rootDir) {
          include("**/*.kt", "**/*.kts", "**/*.java", "**/*.md")
          exclude("**/build/**", "**/.gradle/**", "**/.kotlin/**")
          exclude(excludes)
        }
      )
      root.set(layout.projectDirectory)
      only.set(
        providers
          .gradleProperty(STAGED_FILES_PROPERTY)
          .map { it.split(',').filter(String::isNotBlank) }
          .orElse(emptyList())
      )
      report.set(layout.buildDirectory.file("checkSources.txt"))
    }
  tasks.named("check") { dependsOn(checkSources) }
  return checkSources
}
