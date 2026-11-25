import dev.chieppa.ease.build.WebgpuNatives

plugins {
  id("ease.kotlin-module")
  id("com.google.devtools.ksp")
}

dependencies {
  api(project(":ease-platform"))
  api(project(":ease-memory"))
  api(project(":ease-math"))
  ksp(project(":ease-codegen"))
  kspTest(project(":ease-codegen"))
  // wgpu-native's bindings. The library itself comes in a natives jar per platform: tests take
  // this machine's, and each host takes the one for the platform it is packaged for.
  implementation(libs.webgpu.native.ffm)
  testImplementation(project(":ease-testing"))
  testRuntimeOnly(
    project.dependencies.variantOf(libs.webgpu.native.ffm) {
      classifier(WebgpuNatives.classifier(providers))
    }
  )
}
