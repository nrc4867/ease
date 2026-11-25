package dev.chieppa.ease.render

import dev.chieppa.ease.gpu.Color
import kotlin.math.abs
import kotlin.math.roundToInt

/** Colors as `0xRRGGBB`, compared within rounding. */
internal object Rgb {
  private const val CHANNEL_MAX = 255
  private const val TOLERANCE = 1

  /** [color] rounded to bytes. */
  fun of(color: Color): Int {
    fun channel(value: Float) = (value * CHANNEL_MAX).roundToInt()
    return (channel(color.red) shl 16) or (channel(color.green) shl 8) or channel(color.blue)
  }

  /** Whether every channel of [a] and [b] is within one step. */
  fun close(a: Int, b: Int): Boolean =
    listOf(16, 8, 0).all { shift ->
      abs((a shr shift and CHANNEL_MAX) - (b shr shift and CHANNEL_MAX)) <= TOLERANCE
    }
}
