import dev.chieppa.ease.build.JoltNatives

plugins {
  id("ease.kotlin-module")
  id("org.jetbrains.kotlin.plugin.serialization")
  id("com.google.devtools.ksp")
}

dependencies {
  api(project(":ease-physics"))
  testImplementation(project(":ease-testing"))
  testRuntimeOnly(JoltNatives.natives(providers, libs.versions.jolt.jni.get()))
  ksp(project(":ease-codegen"))
}
