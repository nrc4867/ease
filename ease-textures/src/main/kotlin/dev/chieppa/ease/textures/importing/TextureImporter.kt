package dev.chieppa.ease.textures.importing

import dev.chieppa.ease.assets.importing.CookedAsset
import dev.chieppa.ease.assets.importing.ImportException
import dev.chieppa.ease.assets.importing.Importer
import dev.chieppa.ease.platform.images.ImageDecodeException
import dev.chieppa.ease.platform.images.ImageDecoder
import dev.chieppa.ease.textures.ktx2.Ktx2Writer
import kotlinx.serialization.json.JsonObject

/** The name of the one cooked asset the texture importer writes. */
public const val COOKED_TEXTURE_NAME: String = "texture.ktx2"

/**
 * Imports image files (PNG, JPEG, TGA, Radiance HDR, OpenEXR) as one KTX2 texture,
 * [COOKED_TEXTURE_NAME], cooked by [TextureCooker] under the [TextureSettings] the sidecar and
 * folder presets give.
 */
public object TextureImporter : Importer {
  override val id: String = "texture"

  override val version: Int = 1

  /** The source extensions it registers for. */
  public val extensions: List<String> = listOf("png", "jpg", "jpeg", "tga", "hdr", "exr")

  override fun import(source: ByteArray, settings: JsonObject): List<CookedAsset> {
    val cooked = TextureCooker.cook(decode(source), TextureSettings.of(settings))
    return listOf(CookedAsset(COOKED_TEXTURE_NAME, Ktx2Writer.write(cooked)))
  }

  /** The pixels of an image file; a file that is not one fails the import. */
  internal fun decode(source: ByteArray) =
    try {
      ImageDecoder.decode(source)
    } catch (failure: ImageDecodeException) {
      throw ImportException("Not an image: ${failure.message}", failure)
    }
}
