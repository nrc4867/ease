package dev.chieppa.ease.net

import dev.chieppa.ease.net.join.ContentManifest
import dev.chieppa.ease.net.join.ContentPart
import dev.chieppa.ease.net.session.CloseReason
import dev.chieppa.ease.net.session.JoinHello
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * The join hello's decoder takes hostile bytes: random messages, every truncation of a real hello
 * and every single bit flip are refused with a typed reason, never thrown, and only the real hello
 * joins. The generator is seeded, so every run feeds the same bytes.
 */
class HostileHelloTest {
  private val content = ContentManifest.of(mods = listOf(ContentPart("pack", "1")))
  private val hello = JoinHello.write(TYPES, content, JoinHello.PROTOCOL_VERSION)
  private val random = Random(SEED)

  private fun refusal(bytes: ByteArray, length: Int = bytes.size): CloseReason? =
    JoinHello.refusal(bytes, length, TYPES, content)

  @Test
  fun `the real hello joins`() {
    assertNull(refusal(hello))
  }

  @Test
  fun `random bytes are refused`() {
    repeat(ROUNDS) {
      val bytes = random.nextBytes(random.nextInt(0, JoinHello.BYTES * 2))
      assertNotNull(refusal(bytes), bytes.contentToString())
    }
  }

  @Test
  fun `every truncation is refused as malformed`() {
    for (cut in 0 until hello.size) {
      assertEquals(CloseReason.MALFORMED_HELLO, refusal(hello.copyOf(cut)), "cut at $cut")
    }
  }

  @Test
  fun `a length beyond the bytes held is refused`() {
    assertEquals(CloseReason.MALFORMED_HELLO, refusal(hello, hello.size + 1))
    assertEquals(CloseReason.MALFORMED_HELLO, refusal(hello, -1))
  }

  @Test
  fun `every bit flip is refused, naming the part it hit`() {
    for (bit in 0 until hello.size * Byte.SIZE_BITS) {
      val flipped = hello.copyOf()
      val index = bit / Byte.SIZE_BITS
      flipped[index] = (flipped[index].toInt() xor (1 shl (bit % Byte.SIZE_BITS))).toByte()

      val expected =
        when {
          index < VERSION_AT -> CloseReason.MALFORMED_HELLO
          index < TYPES_AT -> CloseReason.PROTOCOL_MISMATCH
          index < CONTENT_AT -> CloseReason.TYPES_MISMATCH
          else -> CloseReason.CONTENT_MISMATCH
        }
      assertEquals(expected, refusal(flipped), "bit $bit")
    }
  }

  private companion object {
    const val SEED = 26
    const val ROUNDS = 2_000
    const val TYPES = 0x1234_5678_9ABC_DEF0L
    const val VERSION_AT = 4
    const val TYPES_AT = 6
    const val CONTENT_AT = 14
  }
}
