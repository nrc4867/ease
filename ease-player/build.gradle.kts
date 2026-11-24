import dev.chieppa.ease.build.LwjglNatives
import dev.chieppa.ease.build.WebgpuNatives
import dev.chieppa.ease.build.desktopApp

plugins { id("ease.kotlin-module") }

dependencies {
  implementation(project(":ease-runtime"))
  // The native libraries for the platform this player is built and packaged for.
  runtimeOnly(
    project.dependencies.variantOf(libs.webgpu.native.ffm) {
      classifier(WebgpuNatives.classifier(providers))
    }
  )
  for (library in listOf(libs.lwjgl.asProvider(), libs.lwjgl.sdl)) {
    runtimeOnly(
      project.dependencies.variantOf(library) { classifier(LwjglNatives.classifier(providers)) }
    )
  }
}

val main by sourceSets.getting

tasks.register<JavaExec>("runPlayer") {
  group = "application"
  description = "Runs the player until its window is closed."
  classpath = main.runtimeClasspath
  mainClass = "dev.chieppa.ease.player.PlayerKt"
  desktopApp(this)
}

tasks.register<JavaExec>("playerSmoke") {
  group = "verification"
  description = "Draws 120 frames in the player's window, resizing it halfway, and exits."
  classpath = main.runtimeClasspath
  mainClass = "dev.chieppa.ease.player.PlayerKt"
  args("--frames", "120")
  desktopApp(this)
}
