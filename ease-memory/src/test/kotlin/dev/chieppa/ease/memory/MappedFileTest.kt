package dev.chieppa.ease.memory

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import org.junit.jupiter.api.io.TempDir

/** Little-endian reads over a mapped file, bounded by their region. */
class MappedFileTest {
  @TempDir lateinit var folder: Path

  private fun file(): Path {
    val bytes =
      ByteBuffer.allocate(15)
        .order(ByteOrder.LITTLE_ENDIAN)
        .put(0xFE.toByte())
        .putShort(0xBEEF.toShort())
        .putInt(-2)
        .putFloat(1.5f)
        .putInt(0xFFFF_FFF0.toInt())
        .array()
    return folder.resolve("data.bin").also { Files.write(it, bytes) }
  }

  @Test
  fun `each read takes its own width, little-endian`() {
    MappedFile.open(file()).use { mapped ->
      val whole = mapped.whole

      assertEquals(15L, mapped.byteSize)
      assertEquals(0xFE, whole.uint8(0))
      assertEquals(0xBEEF, whole.uint16(1))
      assertEquals(-2, whole.int32(3))
      assertEquals(1.5f, whole.float32(7))
      assertEquals(0xFFFF_FFF0L, whole.uint32(11))
    }
  }

  @Test
  fun `a region reads from its own start and refuses to run past its end`() {
    MappedFile.open(file()).use { mapped ->
      val region = mapped.region(7, 4)

      assertEquals(1.5f, region.float32(0))
      assertEquals(7L, region.fileOffset)
      assertContentEquals(mapped.whole.copyBytes(7, 4), region.copyBytes(0, 4))
      val past = assertFailsWith<IndexOutOfBoundsException> { region.int32(2) }
      assertTrue("offset 9" in past.message.orEmpty(), past.message)
      assertTrue("data.bin" in past.message.orEmpty(), past.message)
    }
  }

  @Test
  fun `an empty file maps to an empty region`() {
    val empty = folder.resolve("empty.bin").also { Files.write(it, ByteArray(0)) }

    MappedFile.open(empty).use { assertEquals(0L, it.byteSize) }
  }
}
