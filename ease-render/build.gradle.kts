import dev.chieppa.ease.build.WebgpuNatives

plugins {
  id("ease.kotlin-module")
  alias(libs.plugins.kotlinSerialization)
}

dependencies {
  api(project(":ease-gpu"))
  api(project(":ease-ecs"))
  testRuntimeOnly(
    project.dependencies.variantOf(libs.webgpu.native.ffm) {
      classifier(WebgpuNatives.classifier(providers))
    }
  )
}
