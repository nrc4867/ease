plugins {
  id("ease.kotlin-module")
  id("org.jetbrains.kotlin.plugin.serialization")
  id("com.google.devtools.ksp")
}

dependencies {
  api(project(":ease-commands"))
  api(project(":ease-scene"))
  api(project(":ease-annotations"))
  testImplementation(project(":ease-testing"))
  ksp(project(":ease-codegen"))
}
