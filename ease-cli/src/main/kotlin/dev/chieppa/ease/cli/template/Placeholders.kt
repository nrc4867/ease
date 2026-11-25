package dev.chieppa.ease.cli.template

import dev.chieppa.ease.cli.CliFailure

/**
 * The values a template's `{{name}}` markers stand for. A marker with no value is an error, so a
 * misspelled one never reaches a generated file.
 */
public class Placeholders(private val values: Map<String, String>) {
  /** [text] with every `{{name}}` replaced by its value. */
  public fun render(text: String): String =
    MARKER.replace(text) { match ->
      val key = match.groupValues[1]
      values[key] ?: throw CliFailure("The template uses {{$key}}, which has no value")
    }

  /** These placeholders and [more], which win on a clash. */
  public operator fun plus(more: Map<String, String>): Placeholders = Placeholders(values + more)

  /** The value of [name], or null. */
  public operator fun get(name: String): String? = values[name]

  private companion object {
    val MARKER = Regex("""\{\{([A-Za-z][A-Za-z0-9]*)}}""")
  }
}
