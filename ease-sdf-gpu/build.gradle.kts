import dev.chieppa.ease.build.JoltNatives
import dev.chieppa.ease.build.WebgpuNatives

// Distance fields on the GPU and in physics: ease-sdf's programs as generated WGSL, surface nets
// in compute passes into meshes the lit pass draws, and meshes turned into static colliders.
plugins {
  id("ease.kotlin-module")
  id("com.google.devtools.ksp")
}

val joltJni = libs.versions.jolt.jni.get()

dependencies {
  api(project(":ease-sdf"))
  api(project(":ease-gpu"))
  api(project(":ease-physics"))
  ksp(project(":ease-codegen"))
  testImplementation(project(":ease-testing"))
  testRuntimeOnly(JoltNatives.natives(providers, joltJni))
  testRuntimeOnly(
    project.dependencies.variantOf(libs.webgpu.native.ffm) {
      classifier(WebgpuNatives.classifier(providers))
    }
  )
}
