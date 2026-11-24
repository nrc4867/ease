package dev.chieppa.ease.build.sources

/**
 * Tests state the property that matters instead of comparing against committed output: no golden or
 * expected-output files, and no flag that rewrites them.
 */
class GoldenFileRule : SourceRule {
  override fun check(file: SourceFile): List<Finding> {
    val flagFindings =
      if (file.isCode) lineFindings(file, REWRITE_FLAG, REWRITE_MESSAGE) else emptyList()
    return pathFindings(file) + flagFindings
  }

  private fun pathFindings(file: SourceFile): List<Finding> {
    if (!file.isTest || !GOLDEN_PATH.containsMatchIn(file.path)) return emptyList()
    val name = file.path.substringAfterLast('/')
    val message = "$name looks like committed expected output; assert a property instead"
    return listOf(Finding(file.path, null, Severity.ERROR, message))
  }

  private companion object {
    const val REWRITE_MESSAGE = "rewrites expected output; assert a property instead"
    val GOLDEN_PATH = Regex("""(?i)(golden|/expected[-_./]|\.expected\.)""")
    val REWRITE_FLAG = Regex("""(?i)update\s*Goldens?\b""")
  }
}
