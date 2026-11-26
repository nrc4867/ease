// The engine's importers: source files turned into cooked assets, registered through the service
// loader like a game's. The glTF importer cooks meshes, materials, textures, skins, clips and a
// prefab.
import dev.chieppa.ease.build.LwjglNatives
import dev.chieppa.ease.build.WebgpuNatives

plugins { id("ease.kotlin-module") }

dependencies {
  api(project(":ease-render"))
  api(project(":ease-gltf"))
  testImplementation(project(":ease-testing"))
  // The image decoders' natives, for the textures the glTF importer cooks in tests.
  for (library in listOf(libs.lwjgl.asProvider(), libs.lwjgl.stb, libs.lwjgl.tinyexr)) {
    testRuntimeOnly(
      project.dependencies.variantOf(library) { classifier(LwjglNatives.classifier(providers)) }
    )
  }
  testRuntimeOnly(
    project.dependencies.variantOf(libs.webgpu.native.ffm) {
      classifier(WebgpuNatives.classifier(providers))
    }
  )
}

// The Khronos sample models the reader's tests use serve these tests too.
sourceSets.test { resources.srcDir(rootDir.resolve("ease-gltf/src/test/resources")) }
