package dev.chieppa.ease.memory

import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout
import java.nio.ByteOrder

private val UINT8 = ValueLayout.JAVA_BYTE
private val UINT16 = ValueLayout.JAVA_SHORT_UNALIGNED.withOrder(ByteOrder.LITTLE_ENDIAN)
private val INT32 = ValueLayout.JAVA_INT_UNALIGNED.withOrder(ByteOrder.LITTLE_ENDIAN)
private val FLOAT32 = ValueLayout.JAVA_FLOAT_UNALIGNED.withOrder(ByteOrder.LITTLE_ENDIAN)
private const val BYTE_MASK = 0xFF
private const val SHORT_MASK = 0xFFFF
private const val INT_MASK = 0xFFFF_FFFFL

/**
 * Read-only little-endian bytes of a [MappedFile]: the whole file or a part of it. Each read is
 * named by what it reads, takes a byte offset from the region's start, and fails, naming the file
 * and the file offset, when it runs past the region's end.
 */
public class MappedRegion
internal constructor(
  private val bytes: MemorySegment,
  private val file: String,
  /** Where the region starts in its file. */
  public val fileOffset: Long,
) {
  /** The region's length in bytes. */
  public val byteSize: Long
    get() = bytes.byteSize()

  /** The unsigned byte at [offset]. */
  public fun uint8(offset: Long): Int =
    read(offset, 1) { bytes.get(UINT8, it).toInt() and BYTE_MASK }

  /** The unsigned 16-bit integer at [offset]. */
  public fun uint16(offset: Long): Int =
    read(offset, Short.SIZE_BYTES) { bytes.get(UINT16, it).toInt() and SHORT_MASK }

  /** The signed 32-bit integer at [offset]. */
  public fun int32(offset: Long): Int = read(offset, Int.SIZE_BYTES) { bytes.get(INT32, it) }

  /** The unsigned 32-bit integer at [offset]. */
  public fun uint32(offset: Long): Long =
    read(offset, Int.SIZE_BYTES) { bytes.get(INT32, it).toLong() and INT_MASK }

  /** The 32-bit float at [offset]. */
  public fun float32(offset: Long): Float =
    read(offset, Float.SIZE_BYTES) { bytes.get(FLOAT32, it) }

  /** The [length] bytes from [offset] as a region of their own. */
  public fun region(offset: Long, length: Long): MappedRegion {
    require(length >= 0) { "A region of $file needs a length of 0 or more, got $length" }
    inRange(offset, length)
    return MappedRegion(bytes.asSlice(offset, length), file, fileOffset + offset)
  }

  /** The [length] bytes from [offset], copied to the heap. */
  public fun copyBytes(offset: Long, length: Int): ByteArray {
    inRange(offset, length.toLong())
    return bytes.asSlice(offset, length.toLong()).toArray(UINT8)
  }

  private inline fun <T> read(offset: Long, size: Int, value: (Long) -> T): T {
    inRange(offset, size.toLong())
    return value(offset)
  }

  private fun inRange(offset: Long, length: Long) {
    if (offset < 0 || offset + length > byteSize) {
      throw IndexOutOfBoundsException(
        "$file: $length bytes at offset ${fileOffset + offset} run past the end of the " +
          "$byteSize bytes from ${fileOffset}"
      )
    }
  }
}
