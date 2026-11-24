// What every engine and game module shares, whatever its language: the layer rule on its
// dependencies, how its tests run, and the pre-commit hook.
import dev.chieppa.ease.build.catalogLibrary
import dev.chieppa.ease.build.layers.layerViolation
import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins { id("com.diffplug.spotless") }

val moduleName = name

// A module depends only on modules in lower layers; a dependency that breaks this fails the build
// as soon as it is declared.
configurations.configureEach {
  dependencies.withType<ProjectDependency>().configureEach {
    layerViolation(moduleName, path.removePrefix(":"))?.let { throw GradleException(it) }
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
