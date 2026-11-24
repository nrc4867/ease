import dev.chieppa.ease.build.WebgpuNatives

plugins { id("ease.kotlin-module") }

dependencies {
  implementation(project(":ease-runtime"))
  // wgpu-native for the platform this player is built and packaged for.
  runtimeOnly(
    project.dependencies.variantOf(libs.webgpu.native.ffm) {
      classifier(WebgpuNatives.classifier(providers))
    }
  )
}
