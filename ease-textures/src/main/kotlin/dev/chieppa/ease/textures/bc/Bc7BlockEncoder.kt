package dev.chieppa.ease.textures.bc

private const val OPAQUE = 255
private const val TWO_SUBSETS = 2
private const val THREE_SUBSETS = 3
private const val TWO_SUBSET_TRIES = 4
private const val THREE_SUBSET_TRIES = 2
private const val ROTATIONS = 4
private const val PARTITIONS = 64
private const val MODE_ZERO_PARTITIONS = 16

/**
 * Chooses and writes one BC7 block. Every block tries mode 6 (one subset, color and alpha
 * together). Opaque blocks also try modes 1 and 3 on the two-subset partitions that suit them best
 * and modes 0 and 2 on the best three-subset ones; blocks with alpha try modes 4 and 5 under every
 * rotation and index selection, and mode 7 on the best two-subset partitions. The block with the
 * least squared error wins, the earlier try on a tie. Reused from block to block, one per thread.
 */
internal class Bc7BlockEncoder {
  private val modes = Bc7ModeEncoder()
  private val estimator = PartitionEstimator()
  private val candidate = Bc7Block()
  private val best = Bc7Block()
  private val partitions = IntArray(TWO_SUBSET_TRIES)
  private val bits = Bc7Bits()
  private var bestError = Int.MAX_VALUE

  /** Encodes the 16 [pixels] (four channels each, row order) into 16 bytes of [out] at [offset]. */
  fun encode(pixels: IntArray, out: ByteArray, offset: Int) {
    bestError = Int.MAX_VALUE
    attempt(pixels, MODE_SIX, 0, 0, 0)
    if (isOpaque(pixels)) opaque(pixels) else translucent(pixels)
    bits.clear()
    best.pack(bits)
    bits.store(out, offset)
  }

  private fun opaque(pixels: IntArray) {
    estimator.rank(pixels, TWO_SUBSETS, PARTITIONS, ALPHA, TWO_SUBSET_TRIES, partitions)
    for (slot in 0 until TWO_SUBSET_TRIES) {
      attempt(pixels, MODE_ONE, partitions[slot], 0, 0)
      attempt(pixels, MODE_THREE, partitions[slot], 0, 0)
    }
    estimator.rank(
      pixels,
      THREE_SUBSETS,
      MODE_ZERO_PARTITIONS,
      ALPHA,
      THREE_SUBSET_TRIES,
      partitions,
    )
    for (slot in 0 until THREE_SUBSET_TRIES) attempt(pixels, MODE_ZERO, partitions[slot], 0, 0)
    estimator.rank(pixels, THREE_SUBSETS, PARTITIONS, ALPHA, THREE_SUBSET_TRIES, partitions)
    for (slot in 0 until THREE_SUBSET_TRIES) attempt(pixels, MODE_TWO, partitions[slot], 0, 0)
  }

  private fun translucent(pixels: IntArray) {
    for (rotation in 0 until ROTATIONS) {
      attempt(pixels, MODE_FIVE, 0, rotation, 0)
      attempt(pixels, MODE_FOUR, 0, rotation, 0)
      attempt(pixels, MODE_FOUR, 0, rotation, 1)
    }
    estimator.rank(pixels, TWO_SUBSETS, PARTITIONS, CHANNELS, TWO_SUBSET_TRIES, partitions)
    for (slot in 0 until TWO_SUBSET_TRIES) attempt(pixels, MODE_SEVEN, partitions[slot], 0, 0)
  }

  private fun attempt(pixels: IntArray, mode: Int, partition: Int, rotation: Int, selection: Int) {
    if (bestError == 0) return
    val error = modes.encode(pixels, Bc7Mode.ALL[mode], partition, rotation, selection, candidate)
    if (error < bestError) {
      bestError = error
      best.copyFrom(candidate)
    }
  }

  private fun isOpaque(pixels: IntArray): Boolean {
    for (pixel in 0 until BLOCK_PIXELS) {
      if (pixels[pixel * CHANNELS + ALPHA] != OPAQUE) return false
    }
    return true
  }

  private companion object {
    const val MODE_ZERO = 0
    const val MODE_ONE = 1
    const val MODE_TWO = 2
    const val MODE_THREE = 3
    const val MODE_FOUR = 4
    const val MODE_FIVE = 5
    const val MODE_SIX = 6
    const val MODE_SEVEN = 7
  }
}
