package dev.chieppa.ease.gpu

import dev.chieppa.ease.platform.PixelSize

/** Bytes a pixel takes in the formats Ease renders into. */
internal const val BYTES_PER_PIXEL = 4

private const val RED_SHIFT = 16
private const val GREEN_SHIFT = 8
private const val CHANNEL = 0xFF

/**
 * A frame read back from the GPU: [size] pixels in [format], rows tightly packed from the top-left
 * corner.
 */
public class Pixels(
  public val size: PixelSize,
  public val format: TextureFormat,
  private val bytes: ByteArray,
) {
  init {
    require(bytes.size == size.width * size.height * BYTES_PER_PIXEL) {
      "${bytes.size} bytes cannot hold $size pixels"
    }
  }

  /** The pixel at column [x] and row [y] as `0xRRGGBB`, whatever the byte order of [format]. */
  public fun rgb(x: Int, y: Int): Int {
    require(x in 0 until size.width && y in 0 until size.height) { "($x, $y) is outside $size" }
    val at = (y * size.width + x) * BYTES_PER_PIXEL
    fun channel(offset: Int) = bytes[at + offset].toInt() and CHANNEL
    val (red, blue) =
      when (format) {
        TextureFormat.RGBA8Unorm -> channel(0) to channel(2)
        TextureFormat.BGRA8Unorm -> channel(2) to channel(0)
      }
    return (red shl RED_SHIFT) or (channel(1) shl GREEN_SHIFT) or blue
  }

  /** A copy of the raw bytes, in [format]'s byte order. */
  public fun toByteArray(): ByteArray = bytes.copyOf()
}
