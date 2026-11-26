// Our own glTF 2.0 reader: the JSON through kotlinx.serialization, buffers as little-endian
// MappedRegion views, every error naming the element it is about. It knows nothing of the engine
// beyond ease-memory; importers turn what it reads into cooked assets.
plugins {
  id("ease.kotlin-module")
  id("org.jetbrains.kotlin.plugin.serialization")
}

dependencies {
  api(project(":ease-memory"))
  api(project(":ease-annotations"))
  api(libs.kotlinx.serialization.json)
}
