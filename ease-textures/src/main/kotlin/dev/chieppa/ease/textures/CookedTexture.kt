package dev.chieppa.ease.textures

import dev.chieppa.ease.platform.images.TextureSizes

/**
 * A texture as the engine loads it: [width] by [height] texels in [encoding], with its mip [levels]
 * from the full size down (each half the one before, rounded down, at least 1), and how it is
 * sampled. A block-compressed texture's sides are whole multiples of its block size, as GPUs need.
 */
public class CookedTexture(
  public val width: Int,
  public val height: Int,
  public val encoding: TextureEncoding,
  public val levels: List<ByteArray>,
  public val filter: TextureFilter = TextureFilter.LINEAR,
  public val wrap: TextureWrap = TextureWrap.REPEAT,
) {
  init {
    require(width > 0 && height > 0) { "A texture has texels; got $width x $height" }
    require(TextureSizes.blockAligned(width, height, encoding.blockSize)) {
      "A $encoding texture's sides are multiples of ${encoding.blockSize}, not $width x $height"
    }
    val most = TextureSizes.levelCount(width, height)
    require(levels.size in 1..most) {
      "A $width x $height texture has 1 to $most levels, not ${levels.size}"
    }
    levels.forEachIndexed { level, bytes ->
      val expected = encoding.levelBytes(levelWidth(level), levelHeight(level))
      require(bytes.size.toLong() == expected) {
        "Level $level of a $width x $height $encoding texture holds $expected bytes, not " +
          "${bytes.size}"
      }
    }
  }

  /** The width of mip level [level]. */
  public fun levelWidth(level: Int): Int = TextureSizes.levelSide(width, level)

  /** The height of mip level [level]. */
  public fun levelHeight(level: Int): Int = TextureSizes.levelSide(height, level)
}
