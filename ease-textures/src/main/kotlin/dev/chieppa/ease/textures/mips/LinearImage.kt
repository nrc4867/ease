package dev.chieppa.ease.textures.mips

/** Floats a pixel takes: red, green, blue and alpha. */
public const val PIXEL_FLOATS: Int = 4

/**
 * An image being cooked: [width] by [height] pixels of red, green, blue and alpha floats, rows from
 * the top. Colors are linear light; data and normal maps hold their stored values from 0 to 1.
 */
public class LinearImage(
  public val width: Int,
  public val height: Int,
  public val rgba: FloatArray,
) {
  init {
    require(width > 0 && height > 0) { "An image has pixels; got $width x $height" }
    require(rgba.size == width * height * PIXEL_FLOATS) {
      "${rgba.size} floats do not hold $width x $height pixels"
    }
  }
}
