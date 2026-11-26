package dev.chieppa.ease.platform.images

/**
 * The sizes textures come in, shared by the cooked form and the GPU: mip chains (each level half
 * the one before, rounded down, at least 1), block-compressed blocks (4 by 4 texels in 16 bytes),
 * and the bytes a level takes. Partial blocks at a level's edge count whole.
 */
public object TextureSizes {
  /** Texels along each side of a BC block. */
  public const val BLOCK_SIZE: Int = 4

  /** Bytes in one BC5, BC6H or BC7 block. */
  public const val BLOCK_BYTES: Int = 16

  /** Bytes in one texel of four 8-bit channels. */
  public const val RGBA8_BYTES: Int = 4

  /** Bytes in one texel of four 16-bit floats. */
  public const val RGBA16F_BYTES: Int = 8

  /** How many levels a full chain for [width] by [height] has, down to 1 by 1. */
  public fun levelCount(width: Int, height: Int): Int =
    Int.SIZE_BITS - Integer.numberOfLeadingZeros(maxOf(width, height))

  /** The side of mip level [level] of a texture whose full side is [size]. */
  public fun levelSide(size: Int, level: Int): Int = maxOf(1, size shr level)

  /** How many blocks of [blockSize] texels cover [texels], counting a partial one. */
  public fun blocks(texels: Int, blockSize: Int): Int =
    if (texels <= 0) 0 else (texels - 1) / blockSize + 1

  /**
   * Bytes a level of [width] by [height] takes in blocks of [blockSize] texels and [blockBytes].
   */
  public fun levelBytes(width: Int, height: Int, blockSize: Int, blockBytes: Int): Long =
    blocks(width, blockSize).toLong() * blocks(height, blockSize) * blockBytes

  /** Whether both sides of [width] by [height] are whole multiples of [blockSize]. */
  public fun blockAligned(width: Int, height: Int, blockSize: Int): Boolean =
    width % blockSize == 0 && height % blockSize == 0
}
