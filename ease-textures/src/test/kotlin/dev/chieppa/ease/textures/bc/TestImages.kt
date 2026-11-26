package dev.chieppa.ease.textures.bc

import java.util.Random
import javax.imageio.ImageIO
import kotlin.math.log10

/** An RGBA8 image for the codec tests. */
class TestImage(val width: Int, val height: Int, val pixels: ByteArray)

/** Images the codec tests compress, and how far a decoded image is from its original. */
object TestImages {
  private const val CHANNELS = 4
  private const val MAX = 255

  /** An image whose pixel at ([x], [y]) has the channels [color] gives, 0 to 255 each. */
  fun of(width: Int, height: Int, color: (Int, Int, Int) -> Int): TestImage {
    val pixels = ByteArray(width * height * CHANNELS)
    for (y in 0 until height) {
      for (x in 0 until width) {
        for (c in 0 until CHANNELS) pixels[(y * width + x) * CHANNELS + c] = color(x, y, c).toByte()
      }
    }
    return TestImage(width, height, pixels)
  }

  /** Opaque noise from a seeded generator. */
  fun noise(width: Int, height: Int, seed: Long): TestImage {
    val random = Random(seed)
    return of(width, height) { _, _, c -> if (c == CHANNELS - 1) MAX else random.nextInt(MAX + 1) }
  }

  /** A test resource PNG under `cc0/` as opaque RGBA8. */
  fun cc0(name: String): TestImage {
    val stream =
      checkNotNull(
        TestImages::class.java.getResourceAsStream("/dev/chieppa/ease/textures/cc0/$name")
      )
    val image = stream.use { ImageIO.read(it) }
    return of(image.width, image.height) { x, y, c ->
      when (c) {
        0 -> (image.getRGB(x, y) shr 16) and MAX
        1 -> (image.getRGB(x, y) shr 8) and MAX
        2 -> image.getRGB(x, y) and MAX
        else -> MAX
      }
    }
  }

  /** Peak signal-to-noise ratio in decibels over [channels], infinite for identical images. */
  fun psnr(original: ByteArray, decoded: ByteArray, channels: IntRange = 0..3): Double {
    var sum = 0.0
    var count = 0
    for (pixel in 0 until original.size / CHANNELS) {
      for (c in channels) {
        val d =
          (original[pixel * CHANNELS + c].toInt() and MAX) -
            (decoded[pixel * CHANNELS + c].toInt() and MAX)
        sum += d * d
        count++
      }
    }
    if (sum == 0.0) return Double.POSITIVE_INFINITY
    return 10 * log10(MAX.toDouble() * MAX / (sum / count))
  }

  /** The largest difference of any channel in [channels] between the two images. */
  fun maxError(original: ByteArray, decoded: ByteArray, channels: IntRange = 0..3): Int {
    var most = 0
    for (pixel in 0 until original.size / CHANNELS) {
      for (c in channels) {
        val d =
          (original[pixel * CHANNELS + c].toInt() and MAX) -
            (decoded[pixel * CHANNELS + c].toInt() and MAX)
        most = maxOf(most, kotlin.math.abs(d))
      }
    }
    return most
  }
}
