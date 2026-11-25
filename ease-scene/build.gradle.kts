plugins {
  id("ease.kotlin-module")
  id("org.jetbrains.kotlin.plugin.serialization")
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
