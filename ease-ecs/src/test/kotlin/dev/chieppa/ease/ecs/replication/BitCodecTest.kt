package dev.chieppa.ease.ecs.replication

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class BitCodecTest {
  private val writer = BitWriter(CAPACITY)
  private val reader = BitReader()

  private fun readBack() = reader.reset(writer.buffer, writer.byteLength)

  @Test
  fun `values of any width read back across byte boundaries`() {
    writer.writeBits(5, 3)
    writer.writeBits(-1, 32)
    writer.writeBoolean(true)
    writer.writeBits(0x1234, 13)
    writer.writeVarLong(300)
    writer.writeVarLong(Long.MAX_VALUE)
    readBack()

    assertEquals(5, reader.readBits(3))
    assertEquals(-1, reader.readBits(32))
    assertTrue(reader.readBoolean())
    assertEquals(0x1234, reader.readBits(13))
    assertEquals(300L, reader.readVarLong())
    assertEquals(Long.MAX_VALUE, reader.readVarLong())
    assertEquals(DecodeStatus.OK, reader.status)
  }

  @Test
  fun `bits written later can patch a count written earlier`() {
    writer.writeBits(0, 10)
    writer.writeBits(7, 4)
    writer.writeBitsAt(0, 600, 10)
    readBack()

    assertEquals(600, reader.readBits(10))
    assertEquals(7, reader.readBits(4))
  }

  @Test
  fun `reading past the end fails as truncated and reads zero after`() {
    writer.writeBits(3, 8)
    readBack()

    assertEquals(3, reader.readBits(8))
    assertEquals(0, reader.readBits(1))
    assertEquals(DecodeStatus.TRUNCATED, reader.status)
    assertEquals(0, reader.readBits(0))
  }

  @Test
  fun `a variable-length number longer than ten groups is malformed`() {
    repeat(11) { writer.writeBits(0xFF, 8) }
    readBack()

    reader.readVarLong()

    assertEquals(DecodeStatus.MALFORMED, reader.status)
  }

  @Test
  fun `writing past the buffer is a bug and throws`() {
    val small = BitWriter(1)
    small.writeBits(0, 8)

    assertFailsWith<IllegalStateException> { small.writeBits(1, 1) }
  }

  private companion object {
    const val CAPACITY = 64
  }
}
