package dev.chieppa.ease.build.sources

/** Code files stay around 300 lines: a warning past [warnOver], a failure past [failOver]. */
class FileLengthRule(private val warnOver: Int = 300, private val failOver: Int = 400) :
  SourceRule {
  override fun check(file: SourceFile): List<Finding> {
    if (!file.isCode) return emptyList()
    val count = file.lines.size
    val severity =
      when {
        count > failOver -> Severity.ERROR
        count > warnOver -> Severity.WARNING
        else -> return emptyList()
      }
    val message = "$count lines, over $warnOver; split it by responsibility"
    return listOf(Finding(file.path, null, severity, message))
  }
}
