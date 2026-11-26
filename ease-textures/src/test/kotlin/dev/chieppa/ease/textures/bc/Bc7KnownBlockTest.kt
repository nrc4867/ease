package dev.chieppa.ease.textures.bc

import kotlin.test.Test
import kotlin.test.assertEquals

/** Blocks laid out by hand from the BC7 specification's bit order, decoded independently. */
class Bc7KnownBlockTest {
  private val weights4 = intArrayOf(0, 4, 9, 13, 17, 21, 26, 30, 34, 38, 43, 47, 51, 55, 60, 64)
  private val weights2 = intArrayOf(0, 21, 43, 64)

  /** Bits appended lowest first, as BC7 lays them out. */
  private class Bits {
    val bytes = ByteArray(16)
    var at = 0

    fun put(value: Int, count: Int) {
      for (bit in 0 until count) {
        if ((value shr bit) and 1 == 1) {
          val i = at + bit
          bytes[i / 8] = (bytes[i / 8].toInt() or (1 shl (i % 8))).toByte()
        }
      }
      at += count
    }
  }

  private fun blend(from: Int, to: Int, weight: Int) =
    ((64 - weight) * from + weight * to + 32) shr 6

  private fun channel(decoded: ByteArray, pixel: Int, c: Int) =
    decoded[pixel * 4 + c].toInt() and 255

  @Test
  fun `a mode 6 block decodes as the specification lays it out`() {
    val bits = Bits()
    bits.put(1 shl 6, 7)
    // Endpoint channels in the order red 0, red 1, green 0, green 1, blue 0, blue 1, alpha 0, alpha
    // 1.
    for (value in intArrayOf(127, 0, 0, 127, 64, 64, 127, 127)) bits.put(value, 7)
    bits.put(1, 1) // p-bit of endpoint 0
    bits.put(0, 1) // p-bit of endpoint 1
    bits.put(0, 3) // the anchor pixel's index, top bit implied 0
    for (pixel in 1 until 16) bits.put(pixel, 4)
    assertEquals(128, bits.at)
    val decoded = Bc7.decode(bits.bytes, 4, 4)
    val low = intArrayOf(255, 1, 129, 255)
    val high = intArrayOf(0, 254, 128, 254)
    for (pixel in 0 until 16) {
      for (c in 0 until 4) {
        assertEquals(
          blend(low[c], high[c], weights4[pixel]),
          channel(decoded, pixel, c),
          "pixel $pixel channel $c",
        )
      }
    }
  }

  @Test
  fun `a mode 5 block rotates alpha into the channel it names`() {
    val bits = Bits()
    bits.put(1 shl 5, 6)
    bits.put(1, 2) // rotation 1: alpha and red swap
    for (value in intArrayOf(0, 127, 10, 10, 127, 0)) bits.put(value, 7)
    bits.put(255, 8)
    bits.put(0, 8)
    bits.put(0, 1)
    for (pixel in 1 until 16) bits.put(pixel % 4, 2)
    bits.put(0, 1)
    for (pixel in 1 until 16) bits.put(3 - pixel % 4, 2)
    assertEquals(128, bits.at)
    val decoded = Bc7.decode(bits.bytes, 4, 4)
    val expand7 = { v: Int -> (v shl 1) or (v shr 6) }
    for (pixel in 0 until 16) {
      val colorIndex = pixel % 4
      val alphaIndex = if (pixel == 0) 0 else 3 - pixel % 4
      val red = blend(expand7(0), expand7(127), weights2[colorIndex])
      val alpha = blend(255, 0, weights2[alphaIndex])
      assertEquals(alpha, channel(decoded, pixel, 0), "pixel $pixel red holds alpha")
      assertEquals(
        blend(expand7(10), expand7(10), weights2[colorIndex]),
        channel(decoded, pixel, 1),
      )
      assertEquals(
        blend(expand7(127), expand7(0), weights2[colorIndex]),
        channel(decoded, pixel, 2),
      )
      assertEquals(red, channel(decoded, pixel, 3), "pixel $pixel alpha holds red")
    }
  }

  @Test
  fun `a mode 1 block uses its partition, anchors and shared p-bits`() {
    val bits = Bits()
    bits.put(1 shl 1, 2)
    bits.put(17, 6) // partition 17: subset 1 is pixels 1, 2, 3 and 7; its anchor is pixel 2
    val ends = intArrayOf(0, 63, 63, 0)
    repeat(3) { for (value in ends) bits.put(value, 6) }
    bits.put(0, 1) // shared p-bit of subset 0
    bits.put(1, 1) // shared p-bit of subset 1
    for (pixel in 0 until 16) bits.put(
      if (pixel == 0 || pixel == 2) 0 else 5,
      if (pixel == 0 || pixel == 2) 2 else 3,
    )
    assertEquals(128, bits.at)
    val decoded = Bc7.decode(bits.bytes, 4, 4)
    val weights3 = intArrayOf(0, 9, 18, 27, 37, 46, 55, 64)
    val subset1 = setOf(1, 2, 3, 7)
    for (pixel in 0 until 16) {
      val index = if (pixel == 0 || pixel == 2) 0 else 5
      // Subset 0 (p-bit 0) runs 0 to 253 (126 of 7 bits); subset 1 (p-bit 1) runs 255 to 2.
      val expected =
        if (pixel in subset1) blend(255, 2, weights3[index]) else blend(0, 253, weights3[index])
      for (c in 0 until 3) assertEquals(expected, channel(decoded, pixel, c), "pixel $pixel")
      assertEquals(255, channel(decoded, pixel, 3))
    }
  }
}
