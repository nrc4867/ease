package dev.chieppa.ease.build.sources

/**
 * Comments describe the code as it stands: nothing about earlier behavior, a fix, a request or a
 * session.
 */
class HistoryWordsRule : SourceRule {
  override fun check(file: SourceFile): List<Finding> {
    if (!file.isCode) return emptyList()
    return file.lines.withIndex().mapNotNull { (index, line) ->
      val comment = commentText(line) ?: return@mapNotNull null
      val word = WORDS.find(comment) ?: return@mapNotNull null
      val message = "\"${word.value}\" in a comment; describe the code as it stands"
      Finding(file.path, index + 1, Severity.ERROR, message)
    }
  }

  /** The comment part of [line], or null when it has none. */
  private fun commentText(line: String): String? {
    val trimmed = line.trimStart()
    if (trimmed.startsWith("*") || trimmed.startsWith("/*")) return trimmed
    val slashes = line.indexOf("//")
    return if (slashes >= 0) line.substring(slashes) else null
  }

  private companion object {
    val WORDS =
      Regex(
        listOf(
            "previously",
            "no longer",
            "now uses",
            "changed to",
            "as requested",
            "used to be",
            "was changed",
          )
          .joinToString("|", prefix = """\b(""", postfix = """)\b"""),
        RegexOption.IGNORE_CASE,
      )
  }
}
