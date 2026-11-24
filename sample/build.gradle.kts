import dev.chieppa.ease.build.desktopApp

// The sample game: a second, unrelated consumer that keeps the engine free of any one game. It runs
// in the player, which finds its GameModule on the classpath; the player brings this machine's
// natives.
plugins {
  id("ease.kotlin-module")
  alias(libs.plugins.kotlinSerialization)
}

dependencies {
  implementation(project(":ease-runtime"))
  runtimeOnly(project(":ease-player"))
  runtimeOnly(project(":ease-agent"))
  testImplementation(project(":ease-testing"))
  testImplementation(project(":ease-agent"))
  testImplementation(testFixtures(project(":ease-agent")))
  testImplementation(libs.kotlinx.coroutines.core)
}

val main by sourceSets.getting

tasks.register<JavaExec>("runSample") {
  group = "application"
  description = "Runs the sample game in the player until its window is closed."
  classpath = main.runtimeClasspath
  mainClass = "dev.chieppa.ease.player.PlayerKt"
  desktopApp(this)
}

tasks.register<JavaExec>("sampleSmoke") {
  group = "verification"
  description = "Runs the sample game in the player for 120 frames, resizing halfway, and exits."
  classpath = main.runtimeClasspath
  mainClass = "dev.chieppa.ease.player.PlayerKt"
  args("--frames", "120")
  desktopApp(this)
}

tasks.register<JavaExec>("sampleHeadless") {
  group = "verification"
  description = "Runs the sample game in the player headless for 600 ticks, input script attached."
  classpath = main.runtimeClasspath
  mainClass = "dev.chieppa.ease.player.PlayerKt"
  args("--headless", "600")
}

tasks.register<JavaExec>("runAgentHost") {
  group = "application"
  description =
    "Serves the sample world to agents over MCP on 127.0.0.1 until stopped; --args='--port <n>'."
  classpath = main.runtimeClasspath
  mainClass = "dev.chieppa.ease.agent.AgentMainKt"
  jvmArgs("--enable-native-access=ALL-UNNAMED")
}
