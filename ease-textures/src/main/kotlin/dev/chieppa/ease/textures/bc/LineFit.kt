package dev.chieppa.ease.textures.bc

private const val POWER_STEPS = 8
private const val TINY = 1e-12

/**
 * Fits a line through a set of pixels' channels by principal component: [mean] and [axis] (unit
 * length, or zero when the pixels are all one color) over [channels] channels starting at a first
 * channel. Uses only plain arithmetic and square roots, so every platform fits the same line.
 * Reused from fit to fit.
 */
internal class LineFit {
  val mean: DoubleArray = DoubleArray(CHANNELS)
  val axis: DoubleArray = DoubleArray(CHANNELS)
  private val covariance = DoubleArray(CHANNELS * CHANNELS)
  private val next = DoubleArray(CHANNELS)

  /**
   * Fits [count] pixels of [pixels] listed in [members], channels [first] until [first] plus
   * [channels]. Returns the sum of squared distances of the pixels from the line.
   */
  fun fit(pixels: IntArray, members: IntArray, count: Int, first: Int, channels: Int): Double {
    mean.fill(0.0)
    covariance.fill(0.0)
    for (m in 0 until count) {
      val at = members[m] * CHANNELS + first
      for (c in 0 until channels) mean[c] += pixels[at + c].toDouble()
    }
    for (c in 0 until channels) mean[c] /= count.toDouble()
    for (m in 0 until count) {
      val at = members[m] * CHANNELS + first
      for (a in 0 until channels) {
        val da = pixels[at + a] - mean[a]
        for (b in a until channels) {
          covariance[a * CHANNELS + b] += da * (pixels[at + b] - mean[b])
        }
      }
    }
    for (a in 0 until channels) for (b in 0 until a) covariance[a * CHANNELS + b] =
      covariance[b * CHANNELS + a]
    return principal(channels)
  }

  /** Finds the principal axis; returns the variance left off it. */
  private fun principal(channels: Int): Double {
    var trace = 0.0
    var widest = 0
    for (c in 0 until channels) {
      val variance = covariance[c * CHANNELS + c]
      trace += variance
      if (variance > covariance[widest * CHANNELS + widest]) widest = c
    }
    val along = if (trace > TINY) powerIterate(channels, widest) else 0.0
    return if (trace - along > 0.0) trace - along else 0.0
  }

  /**
   * Leaves the principal [axis] by power iteration from covariance column [widest], and returns the
   * variance along it; a degenerate spread leaves the axis zero and returns 0.
   */
  private fun powerIterate(channels: Int, widest: Int): Double {
    axis.fill(0.0)
    for (c in 0 until channels) next[c] = covariance[widest * CHANNELS + c]
    var length = normalize(next, channels)
    var step = 0
    while (length > TINY && step < POWER_STEPS) {
      for (c in 0 until channels) axis[c] = next[c]
      for (a in 0 until channels) {
        var sum = 0.0
        for (b in 0 until channels) sum += covariance[a * CHANNELS + b] * axis[b]
        next[a] = sum
      }
      length = normalize(next, channels)
      step++
    }
    if (length <= TINY) {
      axis.fill(0.0)
      return 0.0
    }
    for (c in 0 until channels) axis[c] = next[c]
    return length
  }

  private fun normalize(vector: DoubleArray, channels: Int): Double {
    var sum = 0.0
    for (c in 0 until channels) sum += vector[c] * vector[c]
    val length = Math.sqrt(sum)
    if (length > TINY) for (c in 0 until channels) vector[c] /= length
    return length
  }
}
