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
  // SDL3 for windows, their native handles and gamepads. Only this module imports LWJGL.
  implementation(libs.lwjgl.asProvider())
  implementation(libs.lwjgl.sdl)
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
