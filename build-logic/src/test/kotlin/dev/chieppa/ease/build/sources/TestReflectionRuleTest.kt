package dev.chieppa.ease.build.sources

import kotlin.test.Test
import kotlin.test.assertEquals

class TestReflectionRuleTest {
  private val rule = TestReflectionRule()

  @Test
  fun `widening visibility fails in a test`() {
    val file =
      testSource(
        "field.setAccessible(true)",
        "field.isAccessible = true",
        "A::class.java.getDeclaredField(\"x\")",
        "@VisibleForTesting internal fun x() = 1",
      )

    assertEquals(listOf(1, 2, 3, 4), rule.check(file).map { it.line })
  }

  @Test
  fun `the same code in main sources is left alone`() {
    assertEquals(emptyList(), rule.check(mainSource("field.setAccessible(true)")))
  }
}
