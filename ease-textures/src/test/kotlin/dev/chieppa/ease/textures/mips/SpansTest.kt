package dev.chieppa.ease.textures.mips

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SpansTest {
  @Test
  fun `wide images split every source pixel whole among the targets`() {
    // 70 000 by 50 000 puts index times source past Int.MAX_VALUE.
    val source = 70_000
    val target = 50_000
    val spans = Spans(source, target)
    for (index in listOf(0, 1, 30_678, 30_679, target - 2, target - 1)) {
      var total = 0L
      for (pixel in spans.first(index)..spans.last(index)) {
        val weight = spans.weight(index, pixel)
        assertTrue(weight > 0, "target $index pixel $pixel")
        total += weight
      }
      assertEquals(source.toLong(), total, "target $index covers one target's share")
    }
    assertEquals(source - 1, spans.last(target - 1))
  }
}
