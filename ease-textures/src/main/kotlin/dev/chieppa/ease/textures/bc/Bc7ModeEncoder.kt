package dev.chieppa.ease.textures.bc

private const val OPAQUE = 255

/**
 * Encodes a block in one mode with one partition, rotation and index selection, into a [Bc7Block],
 * and returns its error: the sum of squared channel differences from the pixels. Each subset is
 * fitted on its own; modes 4 and 5 fit color and alpha separately. Anchor pixels are then given
 * indices whose top bit is clear by swapping their subset's endpoints. Reused from block to block.
 */
internal class Bc7ModeEncoder {
  private val subset = SubsetEncoder()
  private val rotated = IntArray(BLOCK_PIXELS * CHANNELS)
  private val members = Array(MAX_SUBSETS) { IntArray(BLOCK_PIXELS) }
  private val counts = IntArray(MAX_SUBSETS)
  private val everyPixel = IntArray(BLOCK_PIXELS) { it }

  /** Encodes [pixels] into [block] as [mode] says and returns the error. */
  @Suppress("LongParameterList") // A block's mode and the choices the mode takes.
  fun encode(
    pixels: IntArray,
    mode: Bc7Mode,
    partition: Int,
    rotation: Int,
    indexSelection: Int,
    block: Bc7Block,
  ): Int {
    block.mode = mode
    block.partition = partition
    block.rotation = rotation
    block.indexSelection = indexSelection
    block.endpoints.fill(0)
    block.pBits.fill(0)
    block.secondary.fill(0)
    val work = rotate(pixels, rotation)
    return if (mode.separateAlpha) separate(work, mode, indexSelection, block)
    else joint(work, mode, block)
  }

  private fun joint(work: IntArray, mode: Bc7Mode, block: Bc7Block): Int {
    val table = Bc7Tables.subsets[mode.subsets - 1][block.partition]
    counts.fill(0)
    for (pixel in 0 until BLOCK_PIXELS) {
      val s = table[pixel]
      members[s][counts[s]++] = pixel
    }
    val channels = if (mode.hasAlpha) CHANNELS else ALPHA
    var error = 0
    for (s in 0 until mode.subsets) {
      subset.encode(
        work,
        members[s],
        counts[s],
        0,
        channels,
        mode.colorBits,
        mode.pBits,
        mode.indexBits,
      )
      error += subset.error
      store(block, s, 0, channels, block.indices, members[s], counts[s])
      block.pBits[s * 2] = subset.pBits[0]
      block.pBits[s * 2 + 1] = subset.pBits[1]
      val anchor = Bc7Tables.anchor(mode.subsets, block.partition, s)
      fixAnchor(block, s, anchor, 0, channels, mode.indexBits, block.indices, members[s], counts[s])
    }
    if (!mode.hasAlpha) {
      for (pixel in 0 until BLOCK_PIXELS) {
        val d = OPAQUE - work[pixel * CHANNELS + ALPHA]
        error += d * d
      }
    }
    return error
  }

  private fun separate(work: IntArray, mode: Bc7Mode, indexSelection: Int, block: Bc7Block): Int {
    val swapped = indexSelection == 1
    val colorIndices = if (swapped) block.secondary else block.indices
    val alphaIndices = if (swapped) block.indices else block.secondary
    val colorBits = if (swapped) mode.secondaryIndexBits else mode.indexBits
    val alphaBits = if (swapped) mode.indexBits else mode.secondaryIndexBits
    subset.encode(work, everyPixel, BLOCK_PIXELS, 0, ALPHA, mode.colorBits, PBits.NONE, colorBits)
    var error = subset.error
    store(block, 0, 0, ALPHA, colorIndices, everyPixel, BLOCK_PIXELS)
    fixAnchor(block, 0, 0, 0, ALPHA, colorBits, colorIndices, everyPixel, BLOCK_PIXELS)
    subset.encode(work, everyPixel, BLOCK_PIXELS, ALPHA, 1, mode.alphaBits, PBits.NONE, alphaBits)
    error += subset.error
    store(block, 0, ALPHA, 1, alphaIndices, everyPixel, BLOCK_PIXELS)
    fixAnchor(block, 0, 0, ALPHA, 1, alphaBits, alphaIndices, everyPixel, BLOCK_PIXELS)
    return error
  }

  /**
   * Copies the subset encoder's endpoints, and the indices of [pixels], into [block]'s subset [s].
   */
  @Suppress("LongParameterList") // The subset, its channels and its pixels, set without an object.
  private fun store(
    block: Bc7Block,
    s: Int,
    first: Int,
    channels: Int,
    indices: IntArray,
    pixels: IntArray,
    count: Int,
  ) {
    for (end in 0 until 2) {
      for (c in 0 until channels) {
        block.endpoints[(s * 2 + end) * CHANNELS + first + c] = subset.quantized[end * CHANNELS + c]
      }
    }
    for (m in 0 until count) indices[pixels[m]] = subset.indices[pixels[m]]
  }

  /**
   * Swaps subset [s]'s endpoints over [channels] channels from [first] and mirrors its pixels'
   * indices when the [anchor] pixel's index has its top bit set, as the format requires.
   */
  @Suppress("LongParameterList") // The subset, its channels and its pixels, set without an object.
  private fun fixAnchor(
    block: Bc7Block,
    s: Int,
    anchor: Int,
    first: Int,
    channels: Int,
    indexBits: Int,
    indices: IntArray,
    pixels: IntArray,
    count: Int,
  ) {
    val top = (1 shl indexBits) - 1
    if (indices[anchor] <= top shr 1) return
    for (c in first until first + channels) {
      val low = (s * 2) * CHANNELS + c
      val high = low + CHANNELS
      val held = block.endpoints[low]
      block.endpoints[low] = block.endpoints[high]
      block.endpoints[high] = held
    }
    val heldBit = block.pBits[s * 2]
    block.pBits[s * 2] = block.pBits[s * 2 + 1]
    block.pBits[s * 2 + 1] = heldBit
    for (m in 0 until count) indices[pixels[m]] = top - indices[pixels[m]]
  }

  private fun rotate(pixels: IntArray, rotation: Int): IntArray {
    if (rotation == 0) return pixels
    pixels.copyInto(rotated)
    val channel = rotation - 1
    for (pixel in 0 until BLOCK_PIXELS) {
      val at = pixel * CHANNELS
      rotated[at + ALPHA] = pixels[at + channel]
      rotated[at + channel] = pixels[at + ALPHA]
    }
    return rotated
  }

  private companion object {
    const val MAX_SUBSETS = 3
  }
}
