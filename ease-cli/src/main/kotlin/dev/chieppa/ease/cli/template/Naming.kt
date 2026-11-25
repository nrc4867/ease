package dev.chieppa.ease.cli.template

import dev.chieppa.ease.cli.CliFailure

/** How one name a person types becomes the forms a project needs: a class, a folder, a package. */
public object Naming {
  private val WORD_BREAK = Regex("""[\s_-]+|(?<=[a-z0-9])(?=[A-Z])""")
  private val NAME = Regex("[A-Za-z][A-Za-z0-9 _-]*")
  private val PACKAGE = Regex("""[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)*""")
  private const val KEYWORD_SUFFIX = "_"
  private val KEYWORDS =
    setOf(
      "as",
      "break",
      "class",
      "continue",
      "do",
      "else",
      "false",
      "for",
      "fun",
      "if",
      "in",
      "interface",
      "is",
      "null",
      "object",
      "package",
      "return",
      "super",
      "this",
      "throw",
      "true",
      "try",
      "typealias",
      "typeof",
      "val",
      "var",
      "when",
      "while",
    )

  /** The lowercase words of [text]: split at spaces, dashes, underscores and camelCase humps. */
  public fun words(text: String): List<String> {
    val parts = text.trim().split(WORD_BREAK)
    return parts.filter(String::isNotEmpty).map(String::lowercase)
  }

  /** `MyGame` from `my game`, `my-game` or `MyGame`. */
  public fun pascal(text: String): String =
    words(text).joinToString("") { word -> word.replaceFirstChar { it.uppercase() } }

  /** `my-game`. */
  public fun kebab(text: String): String = words(text).joinToString("-")

  /** `mygame`, for package segments and serial name prefixes. */
  public fun flat(text: String): String = words(text).joinToString("")

  /** `my game`, for prose. */
  public fun spaced(text: String): String = words(text).joinToString(" ")

  /** [text] when it can name a project, a type or a module; fails otherwise. */
  public fun requireName(text: String): String {
    if (!NAME.matches(text.trim())) {
      throw CliFailure(
        "'$text' is not a name: start with a letter, then letters, digits, spaces, - or _"
      )
    }
    return text.trim()
  }

  /**
   * [text] when it is a lowercase dotted package name none of whose segments is a Kotlin keyword;
   * fails otherwise.
   */
  public fun requirePackage(text: String): String {
    if (!PACKAGE.matches(text)) {
      throw CliFailure("'$text' is not a package: lowercase segments separated by dots")
    }
    val keywords = text.split('.').filter { it in KEYWORDS }
    if (keywords.isNotEmpty()) {
      throw CliFailure("'$text' has a Kotlin keyword as a segment: ${keywords.joinToString()}")
    }
    return text
  }

  /** [root] and the flat form of [name] as a package, with a keyword segment made safe. */
  public fun defaultPackage(root: String, name: String): String {
    val segment = flat(name)
    return "$root.${if (segment in KEYWORDS) segment + KEYWORD_SUFFIX else segment}"
  }
}
