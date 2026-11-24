plugins {
  id("ease.kotlin-module")
  alias(libs.plugins.kotlinSerialization)
  id("com.google.devtools.ksp")
}

dependencies {
  api(project(":ease-assets"))
  api(project(":ease-commands"))
  api(project(":ease-serialization"))
  api(project(":ease-annotations"))
  testImplementation(project(":ease-testing"))
  ksp(project(":ease-codegen"))
}
