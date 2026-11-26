package dev.chieppa.ease.textures.bc

/** Endpoints a block holds at most: two for each of up to three subsets. */
internal const val MAX_ENDPOINTS: Int = 6

/** Channels of a pixel or endpoint: red, green, blue, alpha. */
internal const val CHANNELS: Int = 4

/** The alpha channel's position among a pixel's channels. */
internal const val ALPHA: Int = 3

/**
 * One BC7 block's fields before bit packing: its [mode], partition, rotation and index selection,
 * each endpoint's quantized channels (`endpoints[(subset * 2 + end) * 4 + channel]`, without its
 * p-bit), each endpoint's p-bit (both ends of a subset equal when the mode shares them), and the
 * primary and secondary index of every pixel. Reused from block to block.
 */
internal class Bc7Block {
  var mode: Bc7Mode = Bc7Mode.ALL[MODE_SIX]
  var partition: Int = 0
  var rotation: Int = 0
  var indexSelection: Int = 0
  val endpoints: IntArray = IntArray(MAX_ENDPOINTS * CHANNELS)
  val pBits: IntArray = IntArray(MAX_ENDPOINTS)
  val indices: IntArray = IntArray(BLOCK_PIXELS)
  val secondary: IntArray = IntArray(BLOCK_PIXELS)

  /** Makes this block a copy of [other]. */
  fun copyFrom(other: Bc7Block) {
    mode = other.mode
    partition = other.partition
    rotation = other.rotation
    indexSelection = other.indexSelection
    other.endpoints.copyInto(endpoints)
    other.pBits.copyInto(pBits)
    other.indices.copyInto(indices)
    other.secondary.copyInto(secondary)
  }

  /** Writes this block's 128 bits into [bits], which must be cleared. */
  fun pack(bits: Bc7Bits) {
    val m = mode
    bits.write(1 shl m.number, m.number + 1)
    bits.write(partition, m.partitionBits)
    bits.write(rotation, m.rotationBits)
    bits.write(indexSelection, m.indexSelectionBits)
    val ends = m.subsets * 2
    for (channel in 0 until ALPHA) {
      for (end in 0 until ends) bits.write(endpoints[end * CHANNELS + channel], m.colorBits)
    }
    if (m.hasAlpha)
      for (end in 0 until ends) bits.write(endpoints[end * CHANNELS + ALPHA], m.alphaBits)
    when (m.pBits) {
      PBits.PER_ENDPOINT -> for (end in 0 until ends) bits.write(pBits[end], 1)
      PBits.SHARED -> for (subset in 0 until m.subsets) bits.write(pBits[subset * 2], 1)
      PBits.NONE -> Unit
    }
    writeIndices(bits, indices, m.indexBits, m.subsets)
    if (m.separateAlpha) writeIndices(bits, secondary, m.secondaryIndexBits, 1)
  }

  /**
   * Reads a block from [bits]. Returns false for the reserved mode (a first byte of zero), which
   * decodes as transparent black.
   */
  fun unpack(bits: Bc7Bits): Boolean {
    var number = 0
    while (number < Bc7Mode.ALL.size && bits.read(1) == 0) number++
    if (number == Bc7Mode.ALL.size) return false
    val m = Bc7Mode.ALL[number]
    mode = m
    partition = bits.read(m.partitionBits)
    rotation = bits.read(m.rotationBits)
    indexSelection = bits.read(m.indexSelectionBits)
    val ends = m.subsets * 2
    endpoints.fill(0)
    for (channel in 0 until ALPHA) {
      for (end in 0 until ends) endpoints[end * CHANNELS + channel] = bits.read(m.colorBits)
    }
    if (m.hasAlpha)
      for (end in 0 until ends) endpoints[end * CHANNELS + ALPHA] = bits.read(m.alphaBits)
    pBits.fill(0)
    when (m.pBits) {
      PBits.PER_ENDPOINT -> for (end in 0 until ends) pBits[end] = bits.read(1)
      PBits.SHARED ->
        for (subset in 0 until m.subsets) {
          val bit = bits.read(1)
          pBits[subset * 2] = bit
          pBits[subset * 2 + 1] = bit
        }
      PBits.NONE -> Unit
    }
    readIndices(bits, indices, m.indexBits, m.subsets)
    if (m.separateAlpha) readIndices(bits, secondary, m.secondaryIndexBits, 1)
    else secondary.fill(0)
    return true
  }

  private fun writeIndices(bits: Bc7Bits, values: IntArray, width: Int, subsets: Int) {
    for (pixel in 0 until BLOCK_PIXELS) {
      bits.write(values[pixel], if (isAnchor(pixel, subsets)) width - 1 else width)
    }
  }

  private fun readIndices(bits: Bc7Bits, values: IntArray, width: Int, subsets: Int) {
    for (pixel in 0 until BLOCK_PIXELS) {
      values[pixel] = bits.read(if (isAnchor(pixel, subsets)) width - 1 else width)
    }
  }

  private fun isAnchor(pixel: Int, subsets: Int): Boolean {
    for (subset in 0 until subsets) {
      if (Bc7Tables.anchor(subsets, partition, subset) == pixel) return true
    }
    return false
  }

  private companion object {
    const val MODE_SIX = 6
  }
}
