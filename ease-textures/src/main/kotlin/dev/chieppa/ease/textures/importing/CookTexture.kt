package dev.chieppa.ease.textures.importing

import dev.chieppa.ease.assets.importing.ImportException
import dev.chieppa.ease.textures.TextureUsage
import dev.chieppa.ease.textures.ktx2.Ktx2Writer

/** The image types [cookTexture] takes, as glTF names them. */
public val COOKABLE_IMAGE_TYPES: Set<String> = setOf("image/png", "image/jpeg")

/**
 * Cooks an image held inside another file, such as one embedded in a glTF model, into KTX2 bytes
 * with the default settings for [usage]: [image] is the file's bytes and [mimeType] their type, one
 * of [COOKABLE_IMAGE_TYPES]. An importer adds the result to its cooked assets under a name ending
 * in `.ktx2`, which makes it a texture sub-asset. Throws [ImportException] for another type or
 * bytes that are not an image.
 */
public fun cookTexture(image: ByteArray, mimeType: String, usage: TextureUsage): ByteArray {
  if (mimeType !in COOKABLE_IMAGE_TYPES) {
    throw ImportException("Images of type '$mimeType' are not cooked; use $COOKABLE_IMAGE_TYPES")
  }
  val settings = TextureSettings(usage = usage)
  return Ktx2Writer.write(TextureCooker.cook(TextureImporter.decode(image), settings))
}
