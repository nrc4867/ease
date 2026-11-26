package dev.chieppa.ease.platform.images

/** Four channels a pixel in every decoded image: red, green, blue and alpha. */
public const val IMAGE_CHANNELS: Int = 4

/** An image file's pixels, rows from the top, four channels a pixel. */
public sealed interface DecodedImage {
  /** Pixels across. */
  public val width: Int

  /** Pixels down. */
  public val height: Int

  /**
   * An 8-bit image, as PNG, JPEG and TGA files hold: red, green, blue and alpha bytes, alpha 255
   * where the file has none. The color bytes are as stored, usually sRGB.
   */
  public class Bytes(
    override val width: Int,
    override val height: Int,
    public val rgba: ByteArray,
  ) : DecodedImage

  /**
   * A high dynamic range image, as Radiance HDR and OpenEXR files hold: linear red, green, blue and
   * alpha floats, alpha 1 where the file has none.
   */
  public class Floats(
    override val width: Int,
    override val height: Int,
    public val rgba: FloatArray,
  ) : DecodedImage
}

/** Bytes that are not an image a decoder reads, with the decoder's reason. */
public class ImageDecodeException(message: String) : IllegalArgumentException(message)
