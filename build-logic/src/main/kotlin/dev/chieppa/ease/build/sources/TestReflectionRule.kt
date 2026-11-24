package dev.chieppa.ease.build.sources

/** Tests reach only public API: no reflection to widen visibility, no test-only visibility. */
class TestReflectionRule : SourceRule {
  override fun check(file: SourceFile): List<Finding> {
    if (!file.isTest || !file.isKotlinOrJava) return emptyList()
    return PATTERNS.flatMap { pattern -> lineFindings(file, pattern, MESSAGE) }
  }

  private companion object {
    const val MESSAGE =
      "widens visibility for a test; test public behavior, or extract the part into its own class"

    val PATTERNS =
      listOf(
        Regex("""\b(try)?[sS]etAccessible\b"""),
        Regex("""\bisAccessible\s*=\s*true"""),
        Regex("""\bgetDeclared(Field|Method|Constructor)s?\("""),
        Regex("""@VisibleForTesting\b"""),
      )
  }
}
