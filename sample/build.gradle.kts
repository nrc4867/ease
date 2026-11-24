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
  testImplementation(project(":ease-testing"))
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
