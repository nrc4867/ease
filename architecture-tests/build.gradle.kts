import dev.chieppa.ease.build.layers.layersProperty

// Konsist tests over every module's sources: imports follow the layers in build-logic.
plugins { id("ease.kotlin-module") }

dependencies { testImplementation(libs.konsist) }

tasks.withType<Test>().configureEach {
  systemProperty("ease.layers", layersProperty())
  // Konsist reads the sources, so a change to any of them reruns these tests.
  inputs
    .files(fileTree(rootDir) { include("*/src/**/*.kt", "*/src/**/*.java") })
    .withPathSensitivity(PathSensitivity.RELATIVE)
}
