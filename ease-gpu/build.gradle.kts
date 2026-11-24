import dev.chieppa.ease.build.WebgpuNatives

plugins { id("ease.kotlin-module") }

dependencies {
  api(project(":ease-platform"))
  // wgpu-native's bindings. The library itself comes in a natives jar per platform: tests take
  // this machine's, and each host takes the one for the platform it is packaged for.
  implementation(libs.webgpu.native.ffm)
  testRuntimeOnly(
    project.dependencies.variantOf(libs.webgpu.native.ffm) {
      classifier(WebgpuNatives.classifier(providers))
    }
  )
}
