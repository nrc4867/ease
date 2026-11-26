package dev.chieppa.ease.textures.bc

private const val BYTE_BITS = 8
private const val WEIGHT_TOTAL = 64
private const val WEIGHT_ROUND = 32
private const val WEIGHT_SHIFT = 6

/** How BC7 turns stored endpoint bits into 8-bit values and blends them, shared by both codecs. */
internal object Bc7Values {
  /**
   * The 8-bit value of an endpoint channel stored as [value] in [bits] bits, with [pBit] appended
   * when [withPBit]: the bits are moved to the top and their high bits repeated below.
   */
  fun expand(value: Int, bits: Int, pBit: Int, withPBit: Boolean): Int {
    val full = if (withPBit) (value shl 1) or pBit else value
    val width = if (withPBit) bits + 1 else bits
    if (width >= BYTE_BITS) return full
    val shifted = full shl (BYTE_BITS - width)
    return shifted or (shifted shr width)
  }

  /** The value [weight] sixty-fourths of the way from [from] to [to], rounded as BC7 rounds. */
  fun blend(from: Int, to: Int, weight: Int): Int =
    ((WEIGHT_TOTAL - weight) * from + weight * to + WEIGHT_ROUND) shr WEIGHT_SHIFT
}
