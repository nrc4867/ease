package dev.chieppa.ease.sample

import kotlin.math.abs
import kotlin.math.roundToInt

/** Colors as `0xRRGGBB`, the form the sample's tests compare pixels in. */
internal object Rgb {
  private const val RED_SHIFT = 16
  private const val GREEN_SHIFT = 8
  private const val CHANNEL = 0xFF
  private const val CHANNEL_MAX = 255f
  private const val HEX = 16
  private const val HEX_DIGITS = 6

  /** The color with channels [red], [green] and [blue] from 0 to 1, each rounded to a byte. */
  fun of(red: Float, green: Float, blue: Float): Int =
    (byte(red) shl RED_SHIFT) or (byte(green) shl GREEN_SHIFT) or byte(blue)

  /** Whether every channel of [a] and [b] is within one step, as rounding allows. */
  fun close(a: Int, b: Int): Boolean =
    listOf(RED_SHIFT, GREEN_SHIFT, 0).all { shift ->
      abs((a shr shift and CHANNEL) - (b shr shift and CHANNEL)) <= 1
    }

  /** [rgb] written as `#rrggbb`. */
  fun hex(rgb: Int): String = "#" + rgb.toString(HEX).padStart(HEX_DIGITS, '0')

  /** Which channel of [rgb] is brightest: 0 red, 1 green, 2 blue. */
  fun brightest(rgb: Int): Int {
    val channels = listOf(RED_SHIFT, GREEN_SHIFT, 0).map { rgb shr it and CHANNEL }
    return channels.indexOf(channels.max())
  }

  /** Which of [color]'s red, green and blue is largest. */
  fun brightest(color: FloatArray): Int = color.indexOfFirst { it == color.max() }

  private fun byte(channel: Float): Int = (channel * CHANNEL_MAX).roundToInt()
}
