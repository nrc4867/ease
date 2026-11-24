// What every engine and game module shares, whatever its language: the layer rule on its
// dependencies, how its tests run, and the pre-commit hook.
import dev.chieppa.ease.build.catalogLibrary
import dev.chieppa.ease.build.layers.buildToolViolation
import dev.chieppa.ease.build.layers.layerViolation
import dev.chieppa.ease.build.layers.testSupportViolation
import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins { id("com.diffplug.spotless") }

val moduleName = name

// A module depends only on modules in lower layers, on test-support modules only from its tests,
// and on build tools only from its ksp configurations; a dependency that breaks this fails the
// build as soon as it is declared.
configurations.configureEach {
  val configurationName = name
  dependencies.withType<ProjectDependency>().configureEach {
    val target = path.removePrefix(":")
    val violation =
      layerViolation(moduleName, target)
        ?: testSupportViolation(moduleName, target, configurationName)
        ?: buildToolViolation(moduleName, target, configurationName)
    violation?.let { throw GradleException(it) }
  }
}

dependencies {
  "testImplementation"(platform(catalogLibrary("junit-bom")))
  "testImplementation"(catalogLibrary("junit-jupiter"))
  "testRuntimeOnly"(catalogLibrary("junit-platform-launcher"))
}

val sourceSets = the<SourceSetContainer>()

// Tests named *GpuTest need a WebGPU adapter, so they run only in gpuTest, which CI runs on Mesa's
// software Vulkan driver.
tasks.withType<Test>().configureEach {
  useJUnitPlatform()
  jvmArgs("--enable-native-access=ALL-UNNAMED")
  testLogging.exceptionFormat = TestExceptionFormat.FULL
  // A test that waits for an event that never comes fails here instead of hanging the build.
  systemProperty("junit.jupiter.execution.timeout.default", "5m")
}

tasks.named<Test>("test") { exclude("**/*GpuTest*") }

tasks.register<Test>("gpuTest") {
  group = "verification"
  description = "Runs the tests that need a WebGPU adapter."
  testClassesDirs = sourceSets["test"].output.classesDirs
  classpath = sourceSets["test"].runtimeClasspath
  include("**/*GpuTest*")
}

tasks
  .matching { it.name == "compileJava" || it.name == "compileKotlin" }
  .configureEach {
    dependsOn(":installGitHook")
  }
