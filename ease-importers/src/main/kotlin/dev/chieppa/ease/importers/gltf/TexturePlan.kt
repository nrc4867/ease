package dev.chieppa.ease.importers.gltf

import dev.chieppa.ease.assets.importing.CookedAsset
import dev.chieppa.ease.gltf.GltfFile
import dev.chieppa.ease.gltf.SiblingFiles
import dev.chieppa.ease.gltf.Uris
import dev.chieppa.ease.gltf.fail
import dev.chieppa.ease.gltf.json.TextureRefJson
import dev.chieppa.ease.textures.TextureUsage

/** The extension of cooked textures. */
internal const val TEXTURE_EXTENSION = "ktx2"

/**
 * The textures one import cooks: one per image and use, named after the image (with the use added
 * when one image serves several), cooked through [cooker]. With no cooker no texture is cooked and
 * material slots stay empty.
 */
internal class TexturePlan(
  private val file: GltfFile,
  private val cooker: TextureCooker?,
  names: CookedNames,
) {
  private val uses: Map<Pair<Int, TextureUsage>, String>

  init {
    val wanted = LinkedHashSet<Pair<Int, TextureUsage>>()
    file.root.materials.forEachIndexed { index, material ->
      val pbr = material.pbrMetallicRoughness
      val path = "materials[$index]"
      fun want(ref: TextureRefJson?, usage: TextureUsage, slot: String) {
        if (ref != null) wanted += imageOf(ref.index, "$path.$slot") to usage
      }
      want(pbr.baseColorTexture, TextureUsage.COLOR_SRGB, "pbrMetallicRoughness.baseColorTexture")
      want(pbr.metallicRoughnessTexture, TextureUsage.DATA_LINEAR, "pbrMetallicRoughness")
      want(material.normalTexture, TextureUsage.NORMAL, "normalTexture")
      want(material.occlusionTexture, TextureUsage.DATA_LINEAR, "occlusionTexture")
      want(material.emissiveTexture, TextureUsage.COLOR_SRGB, "emissiveTexture")
    }
    val shared = wanted.groupingBy { it.first }.eachCount()
    uses =
      if (cooker == null) emptyMap()
      else
        wanted.associateWith { (image, usage) ->
          val base =
            file.root.images[image].name
              ?: file.root.images[image].uri?.takeUnless(Uris::isData)?.let(::stem)
          val suffix = if (shared.getValue(image) > 1) "-${usage.name.lowercase()}" else ""
          names.take(base?.let { it + suffix }, "image$image$suffix", TEXTURE_EXTENSION)
        }
  }

  /** The cooked name of texture [texture]'s image for [usage], or null when none is cooked. */
  fun nameOf(texture: Int, usage: TextureUsage): String? =
    uses[imageOf(texture, "textures[$texture]") to usage]

  /** The cooked textures, read through [files], in the order the materials first use them. */
  fun cook(files: SiblingFiles): List<CookedAsset> {
    val cooker = cooker ?: return emptyList()
    return uses.map { (use, name) ->
      val (image, usage) = use
      val bytes = file.image(image, files)
      val mime = bytes.mimeType ?: fail("images[$image]", "has no known MIME type")
      CookedAsset(name, cooker.cook(bytes.bytes, mime, usage))
    }
  }

  private fun imageOf(texture: Int, path: String): Int {
    val json =
      file.root.textures.getOrNull(texture)
        ?: fail(path, "names texture $texture of ${file.root.textures.size}")
    val image = json.source ?: fail("textures[$texture]", "has no image source")
    if (image !in file.root.images.indices) {
      fail(
        "textures[$texture].source",
        "names image $image of ${file.root.images.size}",
      )
    }
    return image
  }

  private fun stem(uri: String): String = uri.substringAfterLast('/').substringBeforeLast('.')
}
