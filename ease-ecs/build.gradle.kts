plugins {
  id("ease.kotlin-module")
  alias(libs.plugins.jmh)
  alias(libs.plugins.kotlinSerialization)
  id("com.google.devtools.ksp")
}

dependencies {
  api(project(":ease-math"))
  api(project(":ease-annotations"))
  api(libs.fleks)
  api(libs.kotlinx.serialization.core)
  testImplementation(project(":ease-testing"))
  ksp(project(":ease-codegen"))
  kspTest(project(":ease-codegen"))
}

// Benchmarks are run on demand (`./gradlew :ease-ecs:jmh`), never by `check`.
jmh {
  resultFormat = "JSON"
  fork = 1
  warmupIterations = 3
  iterations = 5
  warmup = "1s"
  timeOnIteration = "1s"
}
