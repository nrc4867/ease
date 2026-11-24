plugins {
  id("ease.kotlin-module")
  alias(libs.plugins.kotlinSerialization)
}

dependencies {
  api(project(":ease-ecs"))
  api(libs.kotlinx.serialization.json)
  implementation(libs.kotlinx.serialization.cbor)
}
