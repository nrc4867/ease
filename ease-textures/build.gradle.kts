import dev.chieppa.ease.build.LwjglNatives

// Textures as engine assets: the cooked form (KTX2 holding BC7, BC5 or uncompressed levels), its
// importer for image files (decoded through ease-platform, mipmapped and compressed by our own
// encoders, so cooked bytes match on every OS) and its loader. No GPU code.
plugins { id("ease.kotlin-module") }

// The cook check: a program that cooks the test images under several settings and writes the
// hashes, which CI compares across Windows, Linux and macOS.
val main by sourceSets.getting
val cookcheck by sourceSets.creating {
  compileClasspath += main.output
  runtimeClasspath += main.output
}

configurations.named("cookcheckImplementation") { extendsFrom(configurations["implementation"]) }

configurations.named("cookcheckRuntimeOnly") { extendsFrom(configurations["runtimeOnly"]) }

dependencies {
  api(project(":ease-assets"))
  testImplementation(project(":ease-testing"))
  // The image decoders' natives, for tests and the cook check, which import real files.
  for (library in listOf(libs.lwjgl.asProvider(), libs.lwjgl.stb, libs.lwjgl.tinyexr)) {
    val natives =
      project.dependencies.variantOf(library) { classifier(LwjglNatives.classifier(providers)) }
    testRuntimeOnly(natives)
    "cookcheckRuntimeOnly"(natives)
  }
}

tasks.register<JavaExec>("cookManifest") {
  group = "verification"
  description = "Cooks the test images under several settings and writes the hashes of the results."
  classpath = cookcheck.runtimeClasspath
  mainClass = "dev.chieppa.ease.textures.cookcheck.CookManifestKt"
  val images = layout.projectDirectory.dir("src/test/resources/dev/chieppa/ease/textures/cc0")
  val manifest = layout.buildDirectory.file("cook-manifest.txt")
  inputs.dir(images)
  outputs.file(manifest)
  args(images.asFile.path, manifest.get().asFile.path)
  jvmArgs("--enable-native-access=ALL-UNNAMED")
}
