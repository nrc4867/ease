plugins { `kotlin-dsl` }

dependencies {
  implementation(libs.kotlin.gradle.plugin)
  implementation(libs.spotless.gradle.plugin)
  implementation(libs.detekt.gradle.plugin)
  implementation(libs.compose.gradle.plugin)
  implementation(libs.compose.compiler.gradle.plugin)
  implementation(libs.ksp.gradle.plugin)
  implementation(libs.serialization.gradle.plugin)
  testImplementation(kotlin("test"))
  testImplementation(gradleTestKit())
}

tasks.test {
  useJUnitPlatform()
}

// The detekt rules every game is held to are the engine's own: one file, shipped inside the build
// logic for the game convention plugins to write out.
tasks.processResources { from(file("../config/detekt")) { into("ease") } }
