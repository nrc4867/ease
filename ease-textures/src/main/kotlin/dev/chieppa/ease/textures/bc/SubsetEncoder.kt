package dev.chieppa.ease.textures.bc

private const val MAX_VALUE = 255.0
private const val WEIGHT_TOTAL = 64.0
private const val REFINEMENTS = 3
private const val SINGULAR = 1e-9
private const val MAX_LEVELS = 16
private const val P_BIT_PAIRS = 4

/**
 * Encodes one subset of a block: a pair of endpoints over some channels, and an index per pixel.
 * The endpoints start on the pixels' principal line, are quantized under every p-bit choice the
 * mode allows, then refined by least squares against the chosen indices while that lowers the
 * error. Results are [quantized] (`[end * 4 + c]`, c counted from the first fitted channel),
 * [pBits], [indices] by pixel position, and [error], the sum of squared channel differences. Reused
 * from subset to subset.
 */
internal class SubsetEncoder {
  val quantized: IntArray = IntArray(2 * CHANNELS)
  val pBits: IntArray = IntArray(2)
  val indices: IntArray = IntArray(BLOCK_PIXELS)
  var error: Int = Int.MAX_VALUE
    private set

  private val line = LineFit()
  private val low = DoubleArray(CHANNELS)
  private val high = DoubleArray(CHANNELS)
  private val candidate = IntArray(2 * CHANNELS)
  private val candidateIndices = IntArray(BLOCK_PIXELS)
  private val palette = IntArray(MAX_LEVELS * CHANNELS)

  private lateinit var pixels: IntArray
  private lateinit var members: IntArray
  private var count = 0
  private var first = 0
  private var channels = 0
  private var bits = 0
  private var pMode = PBits.NONE
  private var indexBits = 0

  /**
   * Encodes the [count] pixels of [pixels] listed in [members], channels [first] until [first] plus
   * [channels], with endpoint channels of [bits] bits, p-bits kept as [pMode] says, and indices of
   * [indexBits] bits.
   */
  @Suppress("LongParameterList") // One subset's whole description, set without an object.
  fun encode(
    pixels: IntArray,
    members: IntArray,
    count: Int,
    first: Int,
    channels: Int,
    bits: Int,
    pMode: PBits,
    indexBits: Int,
  ) {
    this.pixels = pixels
    this.members = members
    this.count = count
    this.first = first
    this.channels = channels
    this.bits = bits
    this.pMode = pMode
    this.indexBits = indexBits
    error = Int.MAX_VALUE
    startOnLine()
    tryEndpoints()
    var steps = REFINEMENTS
    while (steps > 0 && error > 0 && leastSquares()) {
      val before = error
      tryEndpoints()
      steps = if (error < before) steps - 1 else 0
    }
  }

  private fun startOnLine() {
    line.fit(pixels, members, count, first, channels)
    var lowest = Double.MAX_VALUE
    var highest = -Double.MAX_VALUE
    for (m in 0 until count) {
      val at = members[m] * CHANNELS + first
      var t = 0.0
      for (c in 0 until channels) t += (pixels[at + c] - line.mean[c]) * line.axis[c]
      if (t < lowest) lowest = t
      if (t > highest) highest = t
    }
    for (c in 0 until channels) {
      low[c] = clamp(line.mean[c] + lowest * line.axis[c])
      high[c] = clamp(line.mean[c] + highest * line.axis[c])
    }
  }

  /** Quantizes [low] and [high] under each allowed p-bit choice, keeping the best result. */
  private fun tryEndpoints() {
    when (pMode) {
      PBits.NONE -> tryPBits(0, 0, false)
      PBits.SHARED -> for (p in 0..1) tryPBits(p, p, true)
      PBits.PER_ENDPOINT -> for (p in 0 until P_BIT_PAIRS) tryPBits(p and 1, p shr 1, true)
    }
  }

  private fun tryPBits(lowBit: Int, highBit: Int, withPBit: Boolean) {
    for (c in 0 until channels) {
      candidate[c] = quantize(low[c], lowBit, withPBit)
      candidate[CHANNELS + c] = quantize(high[c], highBit, withPBit)
    }
    val levels = 1 shl indexBits
    val weights = Bc7Tables.weights(indexBits)
    for (c in 0 until channels) {
      val from = Bc7Values.expand(candidate[c], bits, lowBit, withPBit)
      val to = Bc7Values.expand(candidate[CHANNELS + c], bits, highBit, withPBit)
      for (k in 0 until levels) palette[k * CHANNELS + c] = Bc7Values.blend(from, to, weights[k])
    }
    val total = assign(levels)
    if (total < error) {
      error = total
      candidate.copyInto(quantized)
      pBits[0] = lowBit
      pBits[1] = highBit
      for (m in 0 until count) indices[members[m]] = candidateIndices[members[m]]
    }
  }

  /** Gives each pixel its nearest palette entry; stops early once [error] cannot be beaten. */
  private fun assign(levels: Int): Int {
    var total = 0
    for (m in 0 until count) {
      val pixel = members[m]
      val at = pixel * CHANNELS + first
      var best = Int.MAX_VALUE
      var chosen = 0
      for (k in 0 until levels) {
        var distance = 0
        for (c in 0 until channels) {
          val d = palette[k * CHANNELS + c] - pixels[at + c]
          distance += d * d
        }
        if (distance < best) {
          best = distance
          chosen = k
        }
      }
      candidateIndices[pixel] = chosen
      total += best
      if (total >= error) return total
    }
    return total
  }

  /** The stored value whose expansion is nearest [value]. */
  private fun quantize(value: Double, pBit: Int, withPBit: Boolean): Int {
    val width = if (withPBit) bits + 1 else bits
    val scale = ((1 shl width) - 1).toDouble()
    val guess = Math.floor(value * scale / MAX_VALUE + HALF).toInt()
    val center = if (withPBit) Math.floorDiv(guess - pBit, 2) else guess
    val top = (1 shl bits) - 1
    var best = 0
    var bestDistance = Double.MAX_VALUE
    for (q in center - 1..center + 1) {
      val stored = q.coerceIn(0, top)
      val distance = Math.abs(Bc7Values.expand(stored, bits, pBit, withPBit) - value)
      if (distance < bestDistance) {
        bestDistance = distance
        best = stored
      }
    }
    return best
  }

  /** Solves for the endpoints that best fit the chosen indices; false when they cannot move. */
  private fun leastSquares(): Boolean {
    val weights = Bc7Tables.weights(indexBits)
    var a00 = 0.0
    var a01 = 0.0
    var a11 = 0.0
    for (c in 0 until channels) {
      low[c] = 0.0
      high[c] = 0.0
    }
    for (m in 0 until count) {
      val pixel = members[m]
      val w = weights[indices[pixel]] / WEIGHT_TOTAL
      val v = 1.0 - w
      a00 += v * v
      a01 += v * w
      a11 += w * w
      val at = pixel * CHANNELS + first
      for (c in 0 until channels) {
        low[c] += v * pixels[at + c]
        high[c] += w * pixels[at + c]
      }
    }
    val determinant = a00 * a11 - a01 * a01
    if (Math.abs(determinant) < SINGULAR) return false
    for (c in 0 until channels) {
      val b0 = low[c]
      val b1 = high[c]
      low[c] = clamp((a11 * b0 - a01 * b1) / determinant)
      high[c] = clamp((a00 * b1 - a01 * b0) / determinant)
    }
    return true
  }

  private fun clamp(value: Double): Double = value.coerceIn(0.0, MAX_VALUE)

  private companion object {
    const val HALF = 0.5
  }
}
