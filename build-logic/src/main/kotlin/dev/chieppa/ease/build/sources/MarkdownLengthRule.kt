package dev.chieppa.ease.build.sources

/** Markdown files hold one topic each, at most [maxLines] lines. */
class MarkdownLengthRule(private val maxLines: Int = 150) : SourceRule {
  override fun check(file: SourceFile): List<Finding> {
    if (!file.isMarkdown || file.lines.size <= maxLines) return emptyList()
    val message = "${file.lines.size} lines, over $maxLines; split it into one file per topic"
    return listOf(Finding(file.path, null, Severity.ERROR, message))
  }
}
