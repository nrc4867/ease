package dev.chieppa.ease.textures.bc

/** Bytes in a BC4 block. */
internal const val BC4_BLOCK_BYTES: Int = 8

private const val MAX_VALUE = 255
private const val LEVELS = 8
private const val INDEX_BITS = 3
private const val INDEX_MASK = 7
private const val FIRST_INDEX_BYTE = 2
private const val BYTE_BITS = 8
private const val BYTE_MASK = 0xFFL
private const val EIGHT_STEPS = 7
private const val SIX_STEPS = 5
private const val SEARCH = 4

/**
 * One BC4 (unsigned) block: two 8-bit endpoints and a 3-bit index per pixel. When the first
 * endpoint is greater the eight values run evenly between them; otherwise six do, plus 0 and 255.
 * The encoder tries both, searching endpoints a few steps around each one's extremes, and keeps the
 * lower squared error (the lower endpoints on a tie). Reused from block to block.
 */
internal class Bc4Block {
  private val palette = IntArray(LEVELS)
  private val chosen = IntArray(BLOCK_PIXELS)
  private val bestIndices = IntArray(BLOCK_PIXELS)
  private var bestError = Int.MAX_VALUE
  private var bestLow = 0
  private var bestHigh = 0

  /** Encodes the 16 [values] (0 to 255, row order) into 8 bytes of [out] at [offset]. */
  fun encode(values: IntArray, out: ByteArray, offset: Int) {
    var lowest = MAX_VALUE
    var highest = 0
    var innerLowest = MAX_VALUE
    var innerHighest = 0
    for (value in values) {
      lowest = minOf(lowest, value)
      highest = maxOf(highest, value)
      if (value in 1 until MAX_VALUE) {
        innerLowest = minOf(innerLowest, value)
        innerHighest = maxOf(innerHighest, value)
      }
    }
    bestError = Int.MAX_VALUE
    search(values, highest, lowest)
    if (innerLowest > innerHighest) {
      innerLowest = lowest
      innerHighest = lowest
    }
    search(values, innerLowest, innerHighest)
    write(out, offset)
  }

  /** Tries endpoint pairs near ([first], [second]) whose order keeps the mode those two pick. */
  private fun search(values: IntArray, first: Int, second: Int) {
    val eight = first > second
    for (a in first - SEARCH..first + SEARCH) {
      if (a !in 0..MAX_VALUE) continue
      for (b in second - SEARCH..second + SEARCH) {
        if (b !in 0..MAX_VALUE || (a > b) != eight) continue
        val error = evaluate(values, a, b)
        if (error < bestError) {
          bestError = error
          bestLow = a
          bestHigh = b
          chosen.copyInto(bestIndices)
        }
      }
    }
  }

  private fun evaluate(values: IntArray, first: Int, second: Int): Int {
    fill(palette, first, second)
    var total = 0
    for (pixel in 0 until BLOCK_PIXELS) {
      var best = Int.MAX_VALUE
      for (k in 0 until LEVELS) {
        val d = palette[k] - values[pixel]
        if (d * d < best) {
          best = d * d
          chosen[pixel] = k
        }
      }
      total += best
      if (total >= bestError) return total
    }
    return total
  }

  private fun write(out: ByteArray, offset: Int) {
    out[offset] = bestLow.toByte()
    out[offset + 1] = bestHigh.toByte()
    var packed = 0L
    for (pixel in 0 until BLOCK_PIXELS) {
      packed = packed or (bestIndices[pixel].toLong() shl (pixel * INDEX_BITS))
    }
    for (byte in 0 until BC4_BLOCK_BYTES - FIRST_INDEX_BYTE) {
      out[offset + FIRST_INDEX_BYTE + byte] = (packed shr (byte * BYTE_BITS)).toByte()
    }
  }

  /** Writes the 16 values the block at [offset] of [blocks] holds into [values]. */
  fun decode(blocks: ByteArray, offset: Int, values: IntArray) {
    val first = blocks[offset].toInt() and MAX_VALUE
    val second = blocks[offset + 1].toInt() and MAX_VALUE
    fill(palette, first, second)
    var packed = 0L
    for (byte in 0 until BC4_BLOCK_BYTES - FIRST_INDEX_BYTE) {
      packed =
        packed or
          ((blocks[offset + FIRST_INDEX_BYTE + byte].toLong() and BYTE_MASK) shl (byte * BYTE_BITS))
    }
    for (pixel in 0 until BLOCK_PIXELS) {
      values[pixel] = palette[((packed shr (pixel * INDEX_BITS)) and INDEX_MASK.toLong()).toInt()]
    }
  }

  private companion object {
    /** The eight values a block with endpoints [first] and [second] can hold, rounded. */
    fun fill(palette: IntArray, first: Int, second: Int) {
      palette[0] = first
      palette[1] = second
      if (first > second) {
        for (k in 2 until LEVELS) {
          palette[k] = ((LEVELS - k) * first + (k - 1) * second + EIGHT_STEPS / 2) / EIGHT_STEPS
        }
      } else {
        for (k in 2 until LEVELS - 2) {
          palette[k] = ((SIX_STEPS + 1 - k) * first + (k - 1) * second + SIX_STEPS / 2) / SIX_STEPS
        }
        palette[LEVELS - 2] = 0
        palette[LEVELS - 1] = MAX_VALUE
      }
    }
  }
}
