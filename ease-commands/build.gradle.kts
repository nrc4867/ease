plugins {
  id("ease.kotlin-module")
  alias(libs.plugins.kotlinSerialization)
  id("com.google.devtools.ksp")
}

dependencies {
  api(project(":ease-ecs"))
  api(libs.kotlinx.serialization.json)
  testImplementation(project(":ease-testing"))
  ksp(project(":ease-codegen"))
}
