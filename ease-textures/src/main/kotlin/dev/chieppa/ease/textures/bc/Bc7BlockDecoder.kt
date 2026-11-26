package dev.chieppa.ease.textures.bc

/** Turns a [Bc7Block] into its 16 pixels. Reused from block to block. */
internal class Bc7BlockDecoder {
  private val expanded = IntArray(MAX_ENDPOINTS * CHANNELS)

  /** Writes the pixels of [block] into [pixels], four channels a pixel in row order. */
  fun decode(block: Bc7Block, pixels: IntArray) {
    val mode = block.mode
    val withPBit = mode.pBits != PBits.NONE
    for (end in 0 until mode.subsets * 2) {
      val pBit = block.pBits[end]
      for (channel in 0 until CHANNELS) {
        val at = end * CHANNELS + channel
        val bits = if (channel == ALPHA) mode.alphaBits else mode.colorBits
        expanded[at] =
          if (channel == ALPHA && !mode.hasAlpha) {
            MAX_VALUE
          } else {
            Bc7Values.expand(block.endpoints[at], bits, pBit, withPBit)
          }
      }
    }
    val subsets = Bc7Tables.subsets[mode.subsets - 1][block.partition]
    val swapped = block.indexSelection == 1
    val colorIndices = if (swapped) block.secondary else block.indices
    val alphaIndices = if (mode.separateAlpha && !swapped) block.secondary else block.indices
    val colorBits = if (swapped) mode.secondaryIndexBits else mode.indexBits
    val alphaBits =
      when {
        !mode.separateAlpha -> mode.indexBits
        swapped -> mode.indexBits
        else -> mode.secondaryIndexBits
      }
    val colorWeights = Bc7Tables.weights(colorBits)
    val alphaWeights = Bc7Tables.weights(alphaBits)
    for (pixel in 0 until BLOCK_PIXELS) {
      val low = subsets[pixel] * 2 * CHANNELS
      val high = low + CHANNELS
      val colorWeight = colorWeights[colorIndices[pixel]]
      val alphaWeight = alphaWeights[alphaIndices[pixel]]
      val out = pixel * CHANNELS
      for (channel in 0 until ALPHA) {
        pixels[out + channel] =
          Bc7Values.blend(expanded[low + channel], expanded[high + channel], colorWeight)
      }
      pixels[out + ALPHA] =
        Bc7Values.blend(expanded[low + ALPHA], expanded[high + ALPHA], alphaWeight)
      rotate(pixels, out, block.rotation)
    }
  }

  private fun rotate(pixels: IntArray, at: Int, rotation: Int) {
    if (rotation == 0) return
    val channel = at + rotation - 1
    val alpha = pixels[at + ALPHA]
    pixels[at + ALPHA] = pixels[channel]
    pixels[channel] = alpha
  }

  private companion object {
    const val MAX_VALUE = 255
  }
}
