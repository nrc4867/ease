package dev.chieppa.ease.build.sources

/** A file under check: its path relative to the repository root, with `/` separators. */
class SourceFile(val path: String, val lines: List<String>) {
  val isKotlinOrJava: Boolean
    get() = path.endsWith(".kt") || path.endsWith(".java")

  val isCode: Boolean
    get() = isKotlinOrJava || path.endsWith(".kts")

  val isMarkdown: Boolean
    get() = path.endsWith(".md")

  /** Whether the file belongs to a test source set, such as `src/test` or `src/gpuTest`. */
  val isTest: Boolean
    get() = TEST_SOURCE_SET.containsMatchIn(path)

  private companion object {
    val TEST_SOURCE_SET = Regex("""(^|/)src/\w*[tT]est\w*/""")
  }
}

enum class Severity {
  WARNING,
  ERROR,
}

/**
 * One broken rule: where, and what to do about it. [line] is 1-based, or null for the whole file.
 */
data class Finding(val path: String, val line: Int?, val severity: Severity, val message: String) {
  override fun toString(): String = if (line == null) "$path: $message" else "$path:$line: $message"
}

/** A rule no linter covers, checked over one file at a time. */
interface SourceRule {
  fun check(file: SourceFile): List<Finding>
}

/** Findings for each line of [file] that [pattern] matches, with [message] after the match. */
internal fun lineFindings(file: SourceFile, pattern: Regex, message: String): List<Finding> =
  file.lines.withIndex().mapNotNull { (index, line) ->
    val match = pattern.find(line) ?: return@mapNotNull null
    Finding(file.path, index + 1, Severity.ERROR, "${match.value} $message")
  }
