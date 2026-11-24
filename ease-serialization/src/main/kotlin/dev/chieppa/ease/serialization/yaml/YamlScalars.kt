package dev.chieppa.ease.serialization.yaml

private const val CONTROL_LIMIT = 0x20
private const val DELETE = 0x7f
private const val HEX_WIDTH = 4
private const val HEX_RADIX = 16

/** Words a YAML reader may take for something other than a string, so they are always quoted. */
private val RESERVED_WORDS =
  setOf("true", "false", "null", "yes", "no", "on", "off", "y", "n", "~", "nan", "inf")

/**
 * A string that stays a string when written bare: a letter or underscore, then letters, digits,
 * underscores, dots and slashes.
 */
private val PLAIN = Regex("[A-Za-z_][A-Za-z0-9_./]*")

/** How scalars are spelled in canonical YAML: conservative quoting, one spelling for each value. */
internal object YamlScalars {
  /** [text] bare when every reader reads it back as the same string, else double-quoted. */
  fun string(text: String): String =
    if (PLAIN.matches(text) && text.lowercase() !in RESERVED_WORDS) text else quoted(text)

  private fun quoted(text: String): String {
    val out = StringBuilder(text.length + 2).append('"')
    for (char in text) {
      when {
        char == '"' -> out.append("\\\"")
        char == '\\' -> out.append("\\\\")
        char == '\n' -> out.append("\\n")
        char == '\r' -> out.append("\\r")
        char == '\t' -> out.append("\\t")
        char.code < CONTROL_LIMIT || char.code == DELETE -> out.append(unicodeEscape(char))
        else -> out.append(char)
      }
    }
    return out.append('"').toString()
  }

  private fun unicodeEscape(char: Char): String =
    "\\u" + char.code.toString(HEX_RADIX).padStart(HEX_WIDTH, '0')
}
