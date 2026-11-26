package dev.chieppa.ease.textures.mips

private const val LEVELS = 256
private const val MAX_CODE = 255
private const val LINEAR_LIMIT = 0.04045
private const val LINEAR_SLOPE = 12.92
private const val OFFSET = 0.055
private const val SCALE = 1.055
private const val GAMMA = 2.4

/**
 * The sRGB transfer curve between 8-bit codes and linear light, computed once with `StrictMath`, so
 * every machine gets the same values. Encoding picks the code whose encoded value is nearest, found
 * by binary search over the midpoints between codes.
 */
public object SrgbCurve {
  private val toLinear = FloatArray(LEVELS) { decode(it.toDouble() / MAX_CODE).toFloat() }

  /** The linear value at which each code's rounding range ends: between code i and i + 1. */
  private val thresholds = FloatArray(MAX_CODE) { decode((it + 0.5) / MAX_CODE).toFloat() }

  /** The linear light of 8-bit sRGB [code]. */
  public fun toLinear(code: Int): Float = toLinear[code]

  /** The 8-bit sRGB code nearest [linear], which is clamped to 0 to 1. */
  public fun toCode(linear: Float): Int {
    var low = 0
    var high = MAX_CODE
    while (low < high) {
      val middle = (low + high) ushr 1
      if (linear < thresholds[middle]) high = middle else low = middle + 1
    }
    return low
  }

  private fun decode(encoded: Double): Double =
    if (encoded <= LINEAR_LIMIT) {
      encoded / LINEAR_SLOPE
    } else {
      StrictMath.pow((encoded + OFFSET) / SCALE, GAMMA)
    }
}
