plugins { `kotlin-dsl` }

dependencies {
  implementation(libs.kotlin.gradle.plugin)
  implementation(libs.spotless.gradle.plugin)
  implementation(libs.detekt.gradle.plugin)
  testImplementation(kotlin("test"))
}

tasks.test {
  useJUnitPlatform()
}
