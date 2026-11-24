package dev.chieppa.ease.ecs.replication

import kotlin.math.abs
import kotlin.math.ulp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Quantized floats at every width arrive within a step of what was sent, ends included. */
class QuantizationTest {
  private val writer = BitWriter(CAPACITY)
  private val reader = BitReader()

  private fun send(value: Float, quantization: Quantization): Float {
    writer.reset()
    quantization.write(writer, value)
    reader.reset(writer.buffer, writer.byteLength)
    val received = quantization.read(reader)
    assertEquals(DecodeStatus.OK, reader.status)
    return received
  }

  private fun assertArrives(value: Float, quantization: Quantization) {
    val step = (quantization.max - quantization.min) / ((1L shl quantization.bits) - 1)
    val received = send(value, quantization)
    val tolerance = maxOf(step, value.ulp)
    assertTrue(
      abs(received - value) <= tolerance,
      "${quantization.bits} bits: sent $value, received $received",
    )
  }

  @Test
  fun `the top of the range arrives as the top at 25 and 30 bits`() {
    for (bits in WIDE) {
      val quantization = Quantization(bits, MIN, MAX)
      val step = (MAX - MIN) / ((1L shl bits) - 1)

      assertEquals(MAX, send(MAX, quantization), "$bits bits")
      assertArrives(MAX - step, quantization)
      assertArrives(MAX - step * NEAR_TOP, quantization)
    }
  }

  @Test
  fun `the bottom and middle of the range arrive at every width`() {
    for (bits in ReplicatedFields.QUANTIZED_BITS) {
      val quantization = Quantization(bits, MIN, MAX)

      assertEquals(MIN, send(MIN, quantization), "$bits bits")
      assertArrives(MIDDLE, quantization)
      assertArrives(MAX, quantization)
    }
  }

  @Test
  fun `values past the range are clamped to its ends`() {
    val quantization = Quantization(WIDE.last(), MIN, MAX)

    assertEquals(MAX, send(MAX * 2, quantization))
    assertEquals(MIN, send(MIN * 2, quantization))
  }

  private companion object {
    const val CAPACITY = 8
    const val MIN = -50f
    const val MAX = 50f
    const val MIDDLE = 12.5f
    const val NEAR_TOP = 100
    val WIDE = listOf(25, 30)
  }
}
