import dev.chieppa.ease.build.desktopApp
import dev.chieppa.ease.build.headlessApp
import dev.chieppa.ease.build.types.AssembleEaseTypes

// The sample game: a second, unrelated consumer that keeps the engine free of any one game. It runs
// in the player, which finds its GameModule on the classpath; the player brings this machine's
// natives. Its content, the saved scenes it starts from, is the content folder beside this file.
plugins {
  id("ease.kotlin-module")
  id("org.jetbrains.kotlin.plugin.serialization")
  id("com.google.devtools.ksp")
}

dependencies {
  implementation(project(":ease-runtime"))
  implementation(project(":ease-annotations"))
  ksp(project(":ease-codegen"))
  runtimeOnly(project(":ease-player"))
  runtimeOnly(project(":ease-agent"))
  testImplementation(project(":ease-testing"))
  testImplementation(project(":ease-project"))
  testImplementation(project(":ease-agent"))
  testImplementation(testFixtures(project(":ease-agent")))
  testImplementation(libs.kotlinx.coroutines.core)
}

val main by sourceSets.getting

// The folder the game's assets and scenes are in, which every run reads and watches. It is passed
// through an argument provider, so `--args` adds to it rather than replacing it.
val content = layout.projectDirectory.dir("content")

/** `--content <folder>`, capturing only the path, as the configuration cache needs. */
fun contentArgsOf(path: String) = CommandLineArgumentProvider { listOf("--content", path) }

val contentArgs = contentArgsOf(content.asFile.path)

tasks.register<JavaExec>("runSample") {
  group = "application"
  description = "Runs the sample game in the player until its window is closed."
  classpath = main.runtimeClasspath
  mainClass = "dev.chieppa.ease.player.PlayerKt"
  argumentProviders.add(contentArgs)
  desktopApp(this)
}

tasks.register<JavaExec>("sampleSmoke") {
  group = "verification"
  description = "Runs the sample game in the player for 120 frames, resizing halfway, and exits."
  classpath = main.runtimeClasspath
  mainClass = "dev.chieppa.ease.player.PlayerKt"
  args("--frames", "120")
  argumentProviders.add(contentArgs)
  desktopApp(this)
}

tasks.register<JavaExec>("sampleHeadless") {
  group = "verification"
  description = "Runs the sample game in the player headless for 600 ticks, input script attached."
  classpath = main.runtimeClasspath
  mainClass = "dev.chieppa.ease.player.PlayerKt"
  args("--headless", "600")
  argumentProviders.add(contentArgs)
  headlessApp(this)
}

tasks.register<JavaExec>("runAgentHost") {
  group = "application"
  description =
    "Serves the sample world to agents over MCP on 127.0.0.1 until stopped; --args='--port <n>'."
  classpath = main.runtimeClasspath
  mainClass = "dev.chieppa.ease.agent.AgentMainKt"
  argumentProviders.add(contentArgs)
  headlessApp(this)
}

// `ease import` on the sample's content with the sample's own importers: the command line from
// :ease-cli, and the sample's classpath, where the command finds its GameModule.
val easeCli by configurations.creating

dependencies { easeCli(project(":ease-cli")) }

tasks.register<JavaExec>("sampleImport") {
  group = "verification"
  description = "Runs ease import on the sample's content, its palette importer included."
  classpath = main.runtimeClasspath + easeCli
  mainClass = "dev.chieppa.ease.cli.MainKt"
  args("import", "--dir", projectDir.path)
}

// The game's ease.d.ts: its own types joined with every engine module's, for scripts and tests.
val easeTypes =
  tasks.register<AssembleEaseTypes>("assembleEaseTypes") {
    group = "build"
    description = "Writes ease.d.ts from the type declarations of every module the game runs on."
    classpath.from(main.runtimeClasspath)
    output = layout.buildDirectory.file("ease/ease.d.ts")
  }

tasks.withType<Test>().configureEach {
  inputs.file(easeTypes.flatMap { it.output })
  systemProperty("ease.dts", easeTypes.get().output.get().asFile.path)
  // Tests copy the content before opening it, since a run writes caches into its content folder;
  // those caches are not inputs.
  inputs
    .files(content.asFileTree.matching { exclude(".ease/**") })
    .withPropertyName("sampleContent")
    .withPathSensitivity(PathSensitivity.RELATIVE)
  // Relative to the test's working folder, this module's own, so the path is no input.
  systemProperty("ease.sampleContent", content.asFile.relativeTo(projectDir).path)
}
