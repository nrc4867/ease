import dev.chieppa.ease.build.LwjglNatives

plugins { id("ease.kotlin-module") }

dependencies {
  api(project(":ease-assets"))
  api(project(":ease-render"))
  api(project(":ease-importers"))
  api(project(":ease-commands"))
  api(project(":ease-net"))
  testImplementation(project(":ease-testing"))
  // The image decoders' natives, for content whose textures import in tests.
  for (library in listOf(libs.lwjgl.asProvider(), libs.lwjgl.stb, libs.lwjgl.tinyexr)) {
    testRuntimeOnly(
      project.dependencies.variantOf(library) { classifier(LwjglNatives.classifier(providers)) }
    )
  }
}
