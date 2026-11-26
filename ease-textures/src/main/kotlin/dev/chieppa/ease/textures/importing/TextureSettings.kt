package dev.chieppa.ease.textures.importing

import dev.chieppa.ease.assets.importing.ImportException
import dev.chieppa.ease.textures.TextureFilter
import dev.chieppa.ease.textures.TextureUsage
import dev.chieppa.ease.textures.TextureWrap
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull

/** The largest side a texture keeps unless told otherwise: WebGPU's guaranteed 2D limit. */
public const val DEFAULT_MAX_SIZE: Int = 8192

/** What a texture's levels are stored as. */
public enum class TextureCompression {
  /** BC5 for normal maps, RGBA16F for HDR sources, BC7 for everything else. */
  AUTO,

  /** BC7: color and alpha in 4 by 4 blocks of 16 bytes. */
  BC7,

  /** BC5: red and green in 4 by 4 blocks of 16 bytes, for normal maps. */
  BC5,

  /** Uncompressed, 8 bits a channel. */
  RGBA8,

  /** Uncompressed, a 16-bit float a channel. */
  RGBA16F,
}

/**
 * A texture's import settings, from its sidecar and folder presets (keys in parentheses, all
 * optional): what its pixels are (`usage`: `color`, `data` or `normal`; default `color`), whether
 * color is sRGB (`srgb`; default true for color, and only color can be sRGB), the storage
 * (`compression`: `auto`, `bc7`, `bc5`, `rgba8` or `rgba16f`), the largest side kept (`maxSize`,
 * halved down to fit), whether mip levels are made (`mipmaps`, default true), and the sampler
 * (`filter`: `linear` or `nearest`; `wrap`: `repeat`, `clamp` or `mirror`).
 */
public class TextureSettings(
  public val usage: TextureUsage = TextureUsage.COLOR_SRGB,
  public val compression: TextureCompression = TextureCompression.AUTO,
  public val maxSize: Int = DEFAULT_MAX_SIZE,
  public val mipmaps: Boolean = true,
  public val filter: TextureFilter = TextureFilter.LINEAR,
  public val wrap: TextureWrap = TextureWrap.REPEAT,
) {
  init {
    require(maxSize > 0) { "maxSize must be at least 1, not $maxSize" }
  }

  public companion object {
    private val USAGES = setOf("color", "data", "normal")

    /** The settings [json] holds. Throws [ImportException] naming a key whose value is wrong. */
    public fun of(json: JsonObject): TextureSettings =
      TextureSettings(
        usageOf(json),
        choice(json, "compression", TextureCompression.AUTO),
        maxSizeOf(json),
        flag(json, "mipmaps") ?: true,
        choice(json, "filter", TextureFilter.LINEAR),
        choice(json, "wrap", TextureWrap.REPEAT),
      )

    /** The usage `usage` and `srgb` name together; only color can be sRGB, and is by default. */
    private fun usageOf(json: JsonObject): TextureUsage {
      val usageName = text(json, "usage") ?: "color"
      if (usageName !in USAGES) throw ImportException("usage must be one of $USAGES")
      val srgb = flag(json, "srgb") ?: (usageName == "color")
      if (srgb && usageName != "color") throw ImportException("Only color textures are sRGB")
      return when {
        srgb -> TextureUsage.COLOR_SRGB
        usageName == "normal" -> TextureUsage.NORMAL
        else -> TextureUsage.DATA_LINEAR
      }
    }

    private fun maxSizeOf(json: JsonObject): Int {
      val maxSize = number(json, "maxSize") ?: DEFAULT_MAX_SIZE
      if (maxSize < 1) throw ImportException("maxSize must be at least 1, not $maxSize")
      return maxSize
    }

    private fun primitive(json: JsonObject, key: String): JsonPrimitive? =
      json[key]?.let { it as? JsonPrimitive ?: throw ImportException("$key must be a value") }

    private fun text(json: JsonObject, key: String): String? =
      primitive(json, key)?.let {
        if (!it.isString) throw ImportException("$key must be text")
        it.content
      }

    private fun flag(json: JsonObject, key: String): Boolean? =
      primitive(json, key)?.let {
        it.booleanOrNull ?: throw ImportException("$key must be true or false")
      }

    private fun number(json: JsonObject, key: String): Int? =
      primitive(json, key)?.let {
        it.intOrNull ?: throw ImportException("$key must be a whole number")
      }

    private inline fun <reified E : Enum<E>> choice(json: JsonObject, key: String, default: E): E {
      val name = text(json, key) ?: return default
      return enumValues<E>().firstOrNull { it.name.equals(name, ignoreCase = true) }
        ?: throw ImportException(
          "$key must be one of ${enumValues<E>().joinToString { it.name.lowercase() }}"
        )
    }
  }
}
