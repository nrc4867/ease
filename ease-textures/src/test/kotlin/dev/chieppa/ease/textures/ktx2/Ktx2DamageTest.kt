package dev.chieppa.ease.textures.ktx2

import dev.chieppa.ease.textures.CookedTexture
import dev.chieppa.ease.textures.TextureEncoding
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertFailsWith

/** KTX2 files from outside are untrusted: every way they can be wrong is a [Ktx2Exception]. */
class Ktx2DamageTest {
  private val valid =
    Ktx2Writer.write(
      CookedTexture(
        8,
        8,
        TextureEncoding.BC7_UNORM,
        List(4) { level ->
          ByteArray(TextureEncoding.BC7_UNORM.levelBytes(8 shr level, 8 shr level).toInt())
        },
      )
    )

  private fun damaged(at: Int, value: Long, wide: Boolean = false): ByteArray =
    valid.copyOf().also {
      val buffer = ByteBuffer.wrap(it).order(ByteOrder.LITTLE_ENDIAN)
      if (wide) buffer.putLong(at, value) else buffer.putInt(at, value.toInt())
    }

  @Test
  fun `huge counts and lengths are refused before anything is allocated`() {
    assertFailsWith<Ktx2Exception> {
      Ktx2Reader.read(damaged(LEVEL_COUNT_AT, Int.MAX_VALUE.toLong()))
    }
    assertFailsWith<Ktx2Exception> { Ktx2Reader.read(damaged(LEVEL_COUNT_AT, 5)) }
    assertFailsWith<Ktx2Exception> {
      Ktx2Reader.read(damaged(LEVEL_LENGTH_AT, Long.MAX_VALUE, true))
    }
    assertFailsWith<Ktx2Exception> {
      Ktx2Reader.read(damaged(LEVEL_OFFSET_AT, Long.MAX_VALUE, true))
    }
    assertFailsWith<Ktx2Exception> { Ktx2Reader.read(damaged(LEVEL_OFFSET_AT, -16, true)) }
    assertFailsWith<Ktx2Exception> {
      Ktx2Reader.read(damaged(KVD_LENGTH_AT, Int.MAX_VALUE.toLong()))
    }
    assertFailsWith<Ktx2Exception> { Ktx2Reader.read(damaged(WIDTH_AT, Int.MAX_VALUE.toLong())) }
  }

  @Test
  fun `a block-compressed texture whose sides are not multiples of 4 is refused`() {
    assertFailsWith<Ktx2Exception> { Ktx2Reader.read(damaged(WIDTH_AT, 6)) }
  }

  @Test
  fun `randomly damaged files either read or are refused with a Ktx2Exception`() {
    val random = Random(SEED)
    repeat(ROUNDS) {
      val bytes = valid.copyOf()
      repeat(1 + random.nextInt(4)) {
        bytes[random.nextInt(minOf(bytes.size, HEADERS))] = random.nextInt().toByte()
      }
      runCatching { Ktx2Reader.read(bytes) }
        .exceptionOrNull()
        ?.let { failure ->
          if (failure !is Ktx2Exception) throw AssertionError("Round $it threw $failure", failure)
        }
    }
  }

  private companion object {
    const val WIDTH_AT = 20
    const val LEVEL_COUNT_AT = 40
    const val KVD_LENGTH_AT = 60
    const val LEVEL_OFFSET_AT = 80
    const val LEVEL_LENGTH_AT = 88

    /** Damage lands in the header, indexes and key-value data, where it matters. */
    const val HEADERS = 400
    const val ROUNDS = 3000
    const val SEED = 7
  }
}
