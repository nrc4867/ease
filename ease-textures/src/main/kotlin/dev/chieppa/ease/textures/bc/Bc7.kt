package dev.chieppa.ease.textures.bc

/**
 * BC7 block compression of RGBA8 images: 16 bytes for each 4x4 block, blocks in row order. The
 * encoder gives the same bytes on every machine and thread count.
 */
public object Bc7 {
  /**
   * The BC7 blocks of [pixels], [width] by [height] RGBA8 pixels in row order; a block past the
   * image's right or bottom edge repeats the edge pixels. Rows of blocks encode in parallel.
   */
  public fun encode(pixels: ByteArray, width: Int, height: Int): ByteArray {
    BlockPixels.requireImage(pixels, width, height)
    val across = BlockPixels.blocks(width)
    val out = ByteArray(across * BlockPixels.blocks(height) * BC7_BLOCK_BYTES)
    BlockRows.run(BlockPixels.blocks(height)) { by ->
      val encoder = Bc7BlockEncoder()
      val block = IntArray(BLOCK_PIXELS * CHANNELS)
      for (bx in 0 until across) {
        BlockPixels.gather(pixels, width, height, bx, by, block)
        encoder.encode(block, out, (by * across + bx) * BC7_BLOCK_BYTES)
      }
    }
    return out
  }

  /** The [width] by [height] RGBA8 pixels that the BC7 [blocks] hold. */
  public fun decode(blocks: ByteArray, width: Int, height: Int): ByteArray {
    BlockPixels.requireBlocks(blocks, width, height, BC7_BLOCK_BYTES)
    val across = BlockPixels.blocks(width)
    val out = ByteArray(width * height * CHANNELS)
    val bits = Bc7Bits()
    val block = Bc7Block()
    val decoder = Bc7BlockDecoder()
    val pixels = IntArray(BLOCK_PIXELS * CHANNELS)
    for (by in 0 until BlockPixels.blocks(height)) {
      for (bx in 0 until across) {
        bits.load(blocks, (by * across + bx) * BC7_BLOCK_BYTES)
        if (block.unpack(bits)) decoder.decode(block, pixels) else pixels.fill(0)
        BlockPixels.scatter(pixels, out, width, height, bx, by)
      }
    }
    return out
  }
}
