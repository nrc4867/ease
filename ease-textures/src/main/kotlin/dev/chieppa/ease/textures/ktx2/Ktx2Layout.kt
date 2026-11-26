package dev.chieppa.ease.textures.ktx2

/** Sizes and fixed values of the KTX2 container, from the KTX 2.0 specification. */
internal object Ktx2Layout {
  /** The 12 bytes every KTX2 file starts with: «KTX 20», CR, LF, SUB, LF. */
  val IDENTIFIER: ByteArray =
    intArrayOf(0xAB, 0x4B, 0x54, 0x58, 0x20, 0x32, 0x30, 0xBB, 0x0D, 0x0A, 0x1A, 0x0A)
      .map { it.toByte() }
      .toByteArray()

  /** The identifier and the nine 32-bit header fields. */
  const val HEADER_BYTES: Int = 48

  /** The index: four 32-bit and two 64-bit fields. */
  const val INDEX_BYTES: Int = 32

  /** One level index entry: offset, length and uncompressed length, 64 bits each. */
  const val LEVEL_ENTRY_BYTES: Int = 24

  /** Where the level index starts. */
  const val LEVEL_INDEX_AT: Int = HEADER_BYTES + INDEX_BYTES

  /** The key naming the program that wrote the file. */
  const val WRITER_KEY: String = "KTXwriter"

  /** The writer Ease records; it carries no version, so the bytes do not change between builds. */
  const val WRITER: String = "Ease"

  /** The key holding the sampler's filter, `linear` or `nearest`. */
  const val FILTER_KEY: String = "ease.filter"

  /** The key holding the sampler's wrap mode, `repeat`, `clamp` or `mirror`. */
  const val WRAP_KEY: String = "ease.wrap"
}
