// A game module: Kotlin on JDK 25 with explicit API, serialization and the Ease code generators,
// formatting and detekt held to the engine's rules, the engine at the pinned version, and the tasks
// that run the game.
import dev.chieppa.ease.build.catalogLibrary
import dev.chieppa.ease.build.catalogVersion
import dev.chieppa.ease.build.desktopApp
import dev.chieppa.ease.build.game.GameDependencies
import dev.chieppa.ease.build.game.WriteDetektConfig
import dev.chieppa.ease.build.types.AssembleEaseTypes
import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
  id("org.jetbrains.kotlin.jvm")
  id("org.jetbrains.kotlin.plugin.serialization")
  id("com.google.devtools.ksp")
  id("dev.detekt")
  id("com.diffplug.spotless")
}

kotlin {
  jvmToolchain(catalogVersion("jvmToolchain").toInt())
  explicitApi()
}

dependencies {
  implementation(GameDependencies.module(project, "ease-runtime"))
  implementation(GameDependencies.module(project, "ease-annotations"))
  ksp(GameDependencies.module(project, "ease-codegen"))
  runtimeOnly(GameDependencies.module(project, "ease-player"))
  "testImplementation"(kotlin("test"))
  "testImplementation"(platform(catalogLibrary("junit-bom")))
  "testImplementation"(catalogLibrary("junit-jupiter"))
  "testRuntimeOnly"(catalogLibrary("junit-platform-launcher"))
}

spotless {
  lineEndings = com.diffplug.spotless.LineEnding.UNIX
  kotlin {
    targetExclude("build/**")
    ktfmt(catalogVersion("ktfmt")).googleStyle()
  }
  kotlinGradle { ktfmt(catalogVersion("ktfmt")).googleStyle() }
}

val detektConfig =
  tasks.register<WriteDetektConfig>("writeDetektConfig") {
    output = layout.buildDirectory.file("ease/detekt.yml")
  }

detekt {
  buildUponDefaultConfig = true
  config.setFrom(detektConfig.flatMap { it.output })
}

tasks.withType<dev.detekt.gradle.Detekt>().configureEach {
  val generated = layout.buildDirectory.get().asFile
  setSource(source.filter { !it.startsWith(generated) })
}

tasks.withType<Test>().configureEach {
  useJUnitPlatform()
  jvmArgs("--enable-native-access=ALL-UNNAMED")
  testLogging.exceptionFormat = TestExceptionFormat.FULL
}

// Rules such as the forbidden annotations need type resolution, which only the per-source-set
// tasks have.
tasks.named("check") { dependsOn("detektMain", "detektTest") }

val main = the<SourceSetContainer>()["main"]

tasks.register<JavaExec>("runGame") {
  group = "application"
  description = "Runs the game in the player until its window is closed."
  classpath = main.runtimeClasspath
  mainClass = "dev.chieppa.ease.player.PlayerKt"
  desktopApp(this)
}

tasks.register<JavaExec>("runHeadless") {
  group = "application"
  description = "Runs the game in the player with no window or GPU for 600 ticks."
  classpath = main.runtimeClasspath
  mainClass = "dev.chieppa.ease.player.PlayerKt"
  args("--headless", "600")
}

// The game's ease.d.ts: its own types joined with every engine module's, for scripts and tests.
tasks.register<AssembleEaseTypes>("assembleEaseTypes") {
  group = "build"
  description = "Writes ease.d.ts from the type declarations of every module the game runs on."
  classpath.from(main.runtimeClasspath)
  output = layout.buildDirectory.file("ease/ease.d.ts")
}
