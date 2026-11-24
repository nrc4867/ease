plugins {
  id("ease.kotlin-module")
  alias(libs.plugins.kotlinSerialization)
}

dependencies {
  api(libs.kotlinx.serialization.json)
  implementation(libs.snakeyaml.engine)
}
