plugins { `kotlin-dsl` }

dependencies {
  implementation(libs.kotlin.gradle.plugin)
  implementation(libs.spotless.gradle.plugin)
  implementation(libs.detekt.gradle.plugin)
  implementation(libs.compose.gradle.plugin)
  implementation(libs.compose.compiler.gradle.plugin)
  implementation(libs.ksp.gradle.plugin)
  testImplementation(kotlin("test"))
  testImplementation(gradleTestKit())
}

tasks.test {
  useJUnitPlatform()
}
