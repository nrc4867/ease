package dev.chieppa.ease.textures.bc

/** Pixels on each side of a block. */
internal const val BLOCK_SIDE: Int = 4

private const val BYTE_MASK = 0xFF

/** Moves pixels between an RGBA8 image and the 4x4 blocks it is cut into. */
internal object BlockPixels {
  /** Blocks across or down an image [size] pixels wide or tall. */
  fun blocks(size: Int): Int = (size + BLOCK_SIDE - 1) / BLOCK_SIDE

  /**
   * Copies the block at block column [bx] and row [by] of the [width] by [height] image [rgba] into
   * [out] (four channels a pixel); pixels past the image's edge repeat the edge pixel.
   */
  @Suppress("LongParameterList") // An image, a block position and the output, without objects.
  fun gather(rgba: ByteArray, width: Int, height: Int, bx: Int, by: Int, out: IntArray) {
    for (y in 0 until BLOCK_SIDE) {
      val row = minOf(by * BLOCK_SIDE + y, height - 1)
      for (x in 0 until BLOCK_SIDE) {
        val column = minOf(bx * BLOCK_SIDE + x, width - 1)
        val from = (row * width + column) * CHANNELS
        val to = (y * BLOCK_SIDE + x) * CHANNELS
        for (c in 0 until CHANNELS) out[to + c] = rgba[from + c].toInt() and BYTE_MASK
      }
    }
  }

  /** Copies the pixels of [block] that fall inside the image back into [rgba]. */
  @Suppress("LongParameterList") // A block, an image and a block position, without objects.
  fun scatter(block: IntArray, rgba: ByteArray, width: Int, height: Int, bx: Int, by: Int) {
    for (y in 0 until BLOCK_SIDE) {
      val row = by * BLOCK_SIDE + y
      if (row >= height) break
      for (x in 0 until BLOCK_SIDE) {
        val column = bx * BLOCK_SIDE + x
        if (column >= width) break
        val to = (row * width + column) * CHANNELS
        val from = (y * BLOCK_SIDE + x) * CHANNELS
        for (c in 0 until CHANNELS) rgba[to + c] = block[from + c].toByte()
      }
    }
  }

  /** Checks that [pixels] holds [width] by [height] RGBA8 pixels. */
  fun requireImage(pixels: ByteArray, width: Int, height: Int) {
    require(width >= 1 && height >= 1) { "An image is at least 1x1, not ${width}x$height" }
    require(pixels.size == width * height * CHANNELS) {
      "${pixels.size} bytes are not ${width}x$height RGBA8 pixels"
    }
  }

  /** Checks that [blocks] holds the [bytesPerBlock]-byte blocks of a [width] by [height] image. */
  fun requireBlocks(blocks: ByteArray, width: Int, height: Int, bytesPerBlock: Int) {
    require(width >= 1 && height >= 1) { "An image is at least 1x1, not ${width}x$height" }
    require(blocks.size == blocks(width) * blocks(height) * bytesPerBlock) {
      "${blocks.size} bytes are not the blocks of a ${width}x$height image"
    }
  }
}
