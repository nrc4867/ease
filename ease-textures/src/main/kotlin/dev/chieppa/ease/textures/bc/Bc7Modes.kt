@file:Suppress("MagicNumber") // The BC7 specification's mode table, copied as it is.

package dev.chieppa.ease.textures.bc

/** How a BC7 mode keeps its p-bits: none, one per endpoint, or one per subset shared by both. */
internal enum class PBits {
  NONE,
  PER_ENDPOINT,
  SHARED,
}

/**
 * One BC7 mode's layout: [subsets], bits of partition number, rotation and index selection, bits
 * per color and alpha channel of an endpoint (before any p-bit), how p-bits are kept, and the bits
 * of the primary and secondary indices.
 */
@Suppress("LongParameterList") // One field per column of the BC7 mode table.
internal class Bc7Mode(
  val number: Int,
  val subsets: Int,
  val partitionBits: Int,
  val rotationBits: Int,
  val indexSelectionBits: Int,
  val colorBits: Int,
  val alphaBits: Int,
  val pBits: PBits,
  val indexBits: Int,
  val secondaryIndexBits: Int,
) {
  /** Whether alpha and color are interpolated by separate indices (modes 4 and 5). */
  val separateAlpha: Boolean
    get() = secondaryIndexBits > 0

  /** Whether endpoints carry alpha at all; without it alpha decodes as 255. */
  val hasAlpha: Boolean
    get() = alphaBits > 0

  companion object {
    /** The eight modes, indexed by number. */
    val ALL: Array<Bc7Mode> =
      arrayOf(
        Bc7Mode(0, 3, 4, 0, 0, 4, 0, PBits.PER_ENDPOINT, 3, 0),
        Bc7Mode(1, 2, 6, 0, 0, 6, 0, PBits.SHARED, 3, 0),
        Bc7Mode(2, 3, 6, 0, 0, 5, 0, PBits.NONE, 2, 0),
        Bc7Mode(3, 2, 6, 0, 0, 7, 0, PBits.PER_ENDPOINT, 2, 0),
        Bc7Mode(4, 1, 0, 2, 1, 5, 6, PBits.NONE, 2, 3),
        Bc7Mode(5, 1, 0, 2, 0, 7, 8, PBits.NONE, 2, 2),
        Bc7Mode(6, 1, 0, 0, 0, 7, 7, PBits.PER_ENDPOINT, 4, 0),
        Bc7Mode(7, 2, 6, 0, 0, 5, 5, PBits.PER_ENDPOINT, 2, 0),
      )
  }
}
