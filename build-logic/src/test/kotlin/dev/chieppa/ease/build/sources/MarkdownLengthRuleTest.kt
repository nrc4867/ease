package dev.chieppa.ease.build.sources

import kotlin.test.Test
import kotlin.test.assertEquals

class MarkdownLengthRuleTest {
  private val rule = MarkdownLengthRule()

  @Test
  fun `Markdown of 150 lines passes`() {
    assertEquals(emptyList(), rule.check(sourceOfLength("docs/a.md", 150)))
  }

  @Test
  fun `Markdown of 151 lines fails`() {
    val severities = rule.check(sourceOfLength("docs/a.md", 151)).map { it.severity }

    assertEquals(listOf(Severity.ERROR), severities)
  }
}
