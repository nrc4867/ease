import dev.chieppa.ease.build.WebgpuNatives

plugins {
  id("ease.kotlin-module")
  alias(libs.plugins.kotlinSerialization)
  id("com.google.devtools.ksp")
}

dependencies {
  api(project(":ease-gpu"))
  api(project(":ease-ecs"))
  ksp(project(":ease-codegen"))
  testRuntimeOnly(
    project.dependencies.variantOf(libs.webgpu.native.ffm) {
      classifier(WebgpuNatives.classifier(providers))
    }
  )
}
