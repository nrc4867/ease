// The symbol processors that write replicated components' serializers, and the JSON Schemas and
// TypeScript declarations of every serializable type. A build tool: modules take it only in ksp
// configurations, so it never reaches a classpath.
plugins { id("ease.kotlin-module") }

dependencies {
  implementation(libs.ksp.api)
  implementation(libs.kotlinx.serialization.json)
}
