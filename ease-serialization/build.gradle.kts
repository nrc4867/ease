plugins {
  id("ease.kotlin-module")
  id("org.jetbrains.kotlin.plugin.serialization")
}

dependencies {
  api(libs.kotlinx.serialization.json)
  implementation(libs.snakeyaml.engine)
}
