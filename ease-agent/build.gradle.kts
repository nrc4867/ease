import dev.chieppa.ease.build.WebgpuNatives

// The headless agent host: a game's world served to agents over MCP on 127.0.0.1. It runs whichever
// game is on its classpath, so a game owns the run task (see :sample:runAgentHost).
plugins {
  id("ease.kotlin-module")
  // For the test game's own component.
  id("org.jetbrains.kotlin.plugin.serialization")
  id("com.google.devtools.ksp")
  // A plain MCP client over HTTP for this module's and games' tests.
  `java-test-fixtures`
}

dependencies {
  implementation(project(":ease-runtime"))
  // The official MCP Kotlin SDK (MIT and Apache-2.0) and its streamable HTTP transport, served by
  // Ktor's CIO engine (Apache-2.0).
  // Ktor's server core asks for kotlin-reflect, which nothing the host runs needs: left out.
  implementation(libs.mcp.sdk.server) {
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-reflect")
  }
  implementation(libs.ktor.server.cio) {
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-reflect")
  }
  implementation(libs.kotlinx.coroutines.core)
  // The native library for capture on the platform this host is built for.
  runtimeOnly(
    project.dependencies.variantOf(libs.webgpu.native.ffm) {
      classifier(WebgpuNatives.classifier(providers))
    }
  )
  kspTest(project(":ease-codegen"))
  testImplementation(libs.kotlinx.coroutines.test)
  testFixturesImplementation(libs.kotlinx.serialization.json)
}
