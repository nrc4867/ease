import dev.chieppa.ease.build.JoltNatives

plugins {
  id("ease.kotlin-module")
  id("org.jetbrains.kotlin.plugin.serialization")
  id("com.google.devtools.ksp")
}

val joltJni = libs.versions.jolt.jni.get()

dependencies {
  api(project(":ease-scene"))
  api(project(":ease-annotations"))
  // Jolt through jolt-jni. The library itself comes in a natives jar per platform: tests take this
  // machine's, and each host takes the one for the platform it is packaged for.
  implementation(JoltNatives.classes(joltJni))
  testRuntimeOnly(JoltNatives.natives(providers, joltJni))
  testImplementation(project(":ease-testing"))
  testImplementation(libs.kotlinx.serialization.json)
  ksp(project(":ease-codegen"))
}
