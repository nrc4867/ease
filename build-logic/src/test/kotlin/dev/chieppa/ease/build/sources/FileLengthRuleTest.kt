package dev.chieppa.ease.build.sources

import kotlin.test.Test
import kotlin.test.assertEquals

class FileLengthRuleTest {
  private val rule = FileLengthRule()
  private val path = "ease-ecs/src/main/kotlin/A.kt"

  @Test
  fun `a file of 300 lines passes`() {
    assertEquals(emptyList(), rule.check(sourceOfLength(path, 300)))
  }

  @Test
  fun `a file of 301 lines warns`() {
    val severities = rule.check(sourceOfLength(path, 301)).map { it.severity }

    assertEquals(listOf(Severity.WARNING), severities)
  }

  @Test
  fun `a file of 401 lines fails`() {
    val severities = rule.check(sourceOfLength(path, 401)).map { it.severity }

    assertEquals(listOf(Severity.ERROR), severities)
  }

  @Test
  fun `Markdown is left to its own rule`() {
    assertEquals(emptyList(), rule.check(sourceOfLength("docs/a.md", 401)))
  }
}
