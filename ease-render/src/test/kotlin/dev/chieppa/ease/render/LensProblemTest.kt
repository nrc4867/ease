package dev.chieppa.ease.render

import kotlin.test.Test
import kotlin.test.assertEquals

/** Lenses the renderer refuses, and the one it draws through. */
class LensProblemTest {
  @Test
  fun `each lens field out of bounds is named, NaN included`() {
    assertEquals(null, LensProblem.of(60f, 0.1f, 500f))
    assertEquals(LensProblem.FIELD_OF_VIEW, LensProblem.of(0f, 0.1f, 500f))
    assertEquals(LensProblem.FIELD_OF_VIEW, LensProblem.of(180f, 0.1f, 500f))
    assertEquals(LensProblem.FIELD_OF_VIEW, LensProblem.of(Float.NaN, 0.1f, 500f))
    assertEquals(LensProblem.NEAR, LensProblem.of(60f, 0f, 500f))
    assertEquals(LensProblem.FAR, LensProblem.of(60f, 1f, 1f))
  }
}
