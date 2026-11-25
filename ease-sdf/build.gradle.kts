import dev.chieppa.ease.build.JoltNatives
import dev.chieppa.ease.build.WebgpuNatives

// Distance fields: shapes combined with smooth CSG, evaluated in Kotlin and in generated WGSL,
// meshed on the GPU for drawing and on the CPU for static colliders.
plugins {
  id("ease.kotlin-module")
  id("com.google.devtools.ksp")
}

val joltJni = libs.versions.jolt.jni.get()

dependencies {
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
