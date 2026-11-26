import dev.chieppa.ease.build.WebgpuNatives

plugins {
  id("ease.kotlin-module")
  id("org.jetbrains.kotlin.plugin.serialization")
  id("com.google.devtools.ksp")
}

dependencies {
  api(project(":ease-gpu"))
  api(project(":ease-ecs"))
  // Cooked textures and the block decoders for devices without block compression.
  api(project(":ease-textures"))
  testImplementation(project(":ease-testing"))
  ksp(project(":ease-codegen"))
  testRuntimeOnly(
    project.dependencies.variantOf(libs.webgpu.native.ffm) {
      classifier(WebgpuNatives.classifier(providers))
    }
  )
}
