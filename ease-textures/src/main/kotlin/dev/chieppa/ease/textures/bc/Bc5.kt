package dev.chieppa.ease.textures.bc

private const val OPAQUE: Byte = -1
private const val GREEN = 1
private const val BLUE = 2

/**
 * BC5 (unsigned) block compression of two channels, for normal maps and other two-channel data: 16
 * bytes for each 4x4 block, a BC4 block of red then one of green, blocks in row order. The encoder
 * gives the same bytes on every machine and thread count.
 */
public object Bc5 {
  /** Bytes in a BC5 block. */
  private const val BLOCK_BYTES = 2 * BC4_BLOCK_BYTES

  /**
   * The BC5 blocks of the red and green channels of [pixels], [width] by [height] RGBA8 pixels in
   * row order; a block past the image's right or bottom edge repeats the edge pixels. Rows of
   * blocks encode in parallel.
   */
  public fun encode(pixels: ByteArray, width: Int, height: Int): ByteArray {
    BlockPixels.requireImage(pixels, width, height)
    val across = BlockPixels.blocks(width)
    val out = ByteArray(across * BlockPixels.blocks(height) * BLOCK_BYTES)
    BlockRows.run(BlockPixels.blocks(height)) { by ->
      val encoder = Bc4Block()
      val block = IntArray(BLOCK_PIXELS * CHANNELS)
      val channel = IntArray(BLOCK_PIXELS)
      for (bx in 0 until across) {
        BlockPixels.gather(pixels, width, height, bx, by, block)
        val at = (by * across + bx) * BLOCK_BYTES
        for (c in 0..GREEN) {
          for (pixel in 0 until BLOCK_PIXELS) channel[pixel] = block[pixel * CHANNELS + c]
          encoder.encode(channel, out, at + c * BC4_BLOCK_BYTES)
        }
      }
    }
    return out
  }

  /**
   * The [width] by [height] RGBA8 pixels that the BC5 [blocks] hold: red and green from the blocks,
   * blue 0 and alpha 255.
   */
  public fun decode(blocks: ByteArray, width: Int, height: Int): ByteArray {
    BlockPixels.requireBlocks(blocks, width, height, BLOCK_BYTES)
    val across = BlockPixels.blocks(width)
    val out = ByteArray(width * height * CHANNELS)
    val decoder = Bc4Block()
    val channel = IntArray(BLOCK_PIXELS)
    val pixels = IntArray(BLOCK_PIXELS * CHANNELS)
    for (by in 0 until BlockPixels.blocks(height)) {
      for (bx in 0 until across) {
        val at = (by * across + bx) * BLOCK_BYTES
        for (c in 0..GREEN) {
          decoder.decode(blocks, at + c * BC4_BLOCK_BYTES, channel)
          for (pixel in 0 until BLOCK_PIXELS) pixels[pixel * CHANNELS + c] = channel[pixel]
        }
        BlockPixels.scatter(pixels, out, width, height, bx, by)
      }
    }
    for (pixel in 0 until width * height) {
      out[pixel * CHANNELS + BLUE] = 0
      out[pixel * CHANNELS + ALPHA] = OPAQUE
    }
    return out
  }
}
