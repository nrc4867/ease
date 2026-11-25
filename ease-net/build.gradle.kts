plugins {
  id("ease.kotlin-module")
  alias(libs.plugins.kotlinSerialization)
}

dependencies {
  api(project(":ease-commands"))
  testImplementation(project(":ease-testing"))
  testImplementation(project(":ease-scene"))
}
