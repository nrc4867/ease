import dev.chieppa.ease.build.WebgpuNatives
import dev.chieppa.ease.build.desktopApp

// The Phase 1 viewport spike: the engine draws on the device a Stretch GpuSurface owns, and Compose
// draws over it. A throwaway app, never shipped; the editor replaces it.
plugins {
  id("ease.kotlin-module")
  id("org.jetbrains.kotlin.plugin.compose")
  id("org.jetbrains.compose")
}

dependencies {
  implementation(project(":ease-gpu"))
  implementation("dev.chieppa.stretch:stretch-frame")
  implementation("dev.chieppa.stretch:stretch-gpu")
  implementation("dev.chieppa.stretch:stretch-theme")
  implementation("dev.chieppa.stretch:stretch-devtools")
  implementation(compose.desktop.currentOs)
  runtimeOnly(
    project.dependencies.variantOf(libs.webgpu.native.ffm) {
      classifier(WebgpuNatives.classifier(providers))
    }
  )
}

val main by sourceSets.getting

tasks.register<JavaExec>("runSpike") {
  group = "application"
  description = "Opens the viewport spike: the engine's triangle in a Stretch viewport."
  classpath = main.runtimeClasspath
  mainClass = "dev.chieppa.ease.viewport.spike.MainKt"
  desktopApp(this)
  // -Pdevtools=<port> serves screenshots of the window on 127.0.0.1.
  providers.gradleProperty("devtools").orNull?.let { jvmArgs("-Dstretch.devtools=$it") }
}

tasks.register<JavaExec>("measureReadback") {
  group = "verification"
  description = "Times reading a frame back from the GPU at 1080p and 4K, headless."
  classpath = main.runtimeClasspath
  mainClass = "dev.chieppa.ease.viewport.spike.ReadbackCostKt"
  desktopApp(this)
}
