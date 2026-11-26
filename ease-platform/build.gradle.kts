import dev.chieppa.ease.build.LwjglNatives
import dev.chieppa.ease.build.desktopApp

plugins { id("ease.kotlin-module") }

// The smoke apps: small programs that open real windows, run by hand or by CI, never by check.
val main by sourceSets.getting
val smoke by sourceSets.creating {
  compileClasspath += main.output
  runtimeClasspath += main.output
}

configurations.named("smokeImplementation") { extendsFrom(configurations["implementation"]) }

configurations.named("smokeRuntimeOnly") { extendsFrom(configurations["runtimeOnly"]) }

dependencies {
  // Canonical YAML for the players' binding files.
  api(project(":ease-serialization"))
  testImplementation(project(":ease-testing"))
  // SDL3 for windows, their native handles and gamepads. Only this module imports LWJGL.
  implementation(libs.lwjgl.asProvider())
  implementation(libs.lwjgl.sdl)
  // stb_image and tinyexr, for the image decoders importers use; their natives come only with the
  // hosts that import (the command line and the editor), never the player.
  implementation(libs.lwjgl.stb)
  implementation(libs.lwjgl.tinyexr)
  for (library in listOf(libs.lwjgl.asProvider(), libs.lwjgl.stb, libs.lwjgl.tinyexr)) {
    testRuntimeOnly(
      project.dependencies.variantOf(library) { classifier(LwjglNatives.classifier(providers)) }
    )
  }
  for (library in listOf(libs.lwjgl.asProvider(), libs.lwjgl.sdl)) {
    "smokeRuntimeOnly"(
      project.dependencies.variantOf(library) { classifier(LwjglNatives.classifier(providers)) }
    )
  }
}

tasks.register<JavaExec>("windowSmoke") {
  group = "verification"
  description = "Opens a window, resizes it, and closes it; fails when any step does."
  classpath = smoke.runtimeClasspath
  mainClass = "dev.chieppa.ease.platform.smoke.WindowSmokeKt"
  desktopApp(this)
}

tasks.register<JavaExec>("inputSmoke") {
  group = "verification"
  description =
    "Opens a window; keyboard (WASD, arrows) and gamepad (left stick, d-pad) move a camera whose " +
      "position and active device print as it moves. Escape, gamepad start or closing the window ends it."
  classpath = smoke.runtimeClasspath
  mainClass = "dev.chieppa.ease.platform.smoke.InputSmokeKt"
  desktopApp(this)
}
