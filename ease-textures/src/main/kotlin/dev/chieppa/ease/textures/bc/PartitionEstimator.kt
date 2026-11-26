package dev.chieppa.ease.textures.bc

/**
 * Ranks a mode's partitions for a block by how well each subset's pixels lie on one line: the sum
 * of their squared distances from it. Only the best few are then encoded in full. Ties keep the
 * lower partition first, so the ranking is the same everywhere. Reused from block to block.
 */
internal class PartitionEstimator {
  private val line = LineFit()
  private val members = IntArray(BLOCK_PIXELS)
  private val scores = DoubleArray(MAX_PARTITIONS)

  /**
   * Writes into [best] the [keep] lowest-scoring of the first [partitions] partitions with
   * [subsets] subsets for [pixels] over [channels] channels.
   */
  @Suppress("LongParameterList") // The pixels, what to rank, and where the best go.
  fun rank(
    pixels: IntArray,
    subsets: Int,
    partitions: Int,
    channels: Int,
    keep: Int,
    best: IntArray,
  ) {
    val tables = Bc7Tables.subsets[subsets - 1]
    for (p in 0 until partitions) {
      val table = tables[p]
      var score = 0.0
      for (s in 0 until subsets) {
        var count = 0
        for (pixel in 0 until BLOCK_PIXELS) if (table[pixel] == s) members[count++] = pixel
        score += line.fit(pixels, members, count, 0, channels)
      }
      scores[p] = score
    }
    for (slot in 0 until keep) {
      var chosen = -1
      for (p in 0 until partitions) {
        if (taken(best, slot, p)) continue
        if (chosen < 0 || scores[p] < scores[chosen]) chosen = p
      }
      best[slot] = chosen
    }
  }

  private fun taken(best: IntArray, filled: Int, partition: Int): Boolean {
    for (slot in 0 until filled) if (best[slot] == partition) return true
    return false
  }

  private companion object {
    const val MAX_PARTITIONS = 64
  }
}
