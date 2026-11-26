package dev.chieppa.ease.gltf

/** The Khronos sample files in this module's test resources, read as a file beside nothing. */
internal object Samples {
  private val noSiblings = SiblingFiles { error("no file beside it: $it") }

  fun bytes(name: String): ByteArray =
    checkNotNull(Samples::class.java.getResourceAsStream("/khronos/$name")) { name }
      .use {
        it.readBytes()
      }

  fun read(name: String): GltfFile = GltfFile.read(bytes(name), name, noSiblings)

  fun json(text: String, files: SiblingFiles = noSiblings): GltfFile =
    GltfFile.read(text.trimIndent().toByteArray(), "test.gltf", files)
}
