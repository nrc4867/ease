plugins {
  id("ease.kotlin-module")
  alias(libs.plugins.jmh)
}

dependencies {
  api(project(":ease-math"))
  api(libs.fleks)
  testImplementation(project(":ease-testing"))
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
