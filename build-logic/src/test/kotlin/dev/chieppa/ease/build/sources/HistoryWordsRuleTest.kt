package dev.chieppa.ease.build.sources

import kotlin.test.Test
import kotlin.test.assertEquals

class HistoryWordsRuleTest {
  private val rule = HistoryWordsRule()

  @Test
  fun `history words in comments fail`() {
    val file =
      mainSource(
        "// Previously this ran on the main thread.",
        "val x = 1 // no longer cached",
        " * Now uses the job graph.",
        "/* Changed to a fixed step. */",
        "// As requested, the clock is injected.",
      )

    assertEquals(listOf(1, 2, 3, 4, 5), rule.check(file).map { it.line })
  }

  @Test
  fun `the same words outside comments pass`() {
    val file = mainSource("val message = \"no longer available\"", "// Runs on the job graph.")

    assertEquals(emptyList(), rule.check(file))
  }
}
