// The symbol processor that writes replicated components' serializers. A build tool: modules take
// it only in ksp configurations, so it never reaches a classpath.
plugins { id("ease.kotlin-module") }

dependencies { implementation(libs.ksp.api) }
