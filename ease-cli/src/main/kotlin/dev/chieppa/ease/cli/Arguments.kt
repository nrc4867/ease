package dev.chieppa.ease.cli

/**
 * The words after a command's name: positional arguments and `--option value` (or `--option=value`)
 * pairs, in any order.
 */
public class Arguments
private constructor(
  /** The words that are not options, in order. */
  public val positional: List<String>,
  private val options: Map<String, String>,
) {
  /** The value of `--[name]`, or null when it was not given. */
  public fun option(name: String): String? = options[name]

  /** Fails when an option other than [allowed] was given. */
  public fun requireOnly(allowed: Set<String>) {
    val unknown = options.keys - allowed
    if (unknown.isNotEmpty()) {
      throw CliFailure("Unknown option ${unknown.joinToString { "--$it" }}")
    }
  }

  /** The positional arguments, which must number exactly [count]. */
  public fun positional(count: Int, usage: String): List<String> {
    if (positional.size != count) throw CliFailure("Expected $usage")
    return positional
  }

  /** Reading the words of a command line. */
  public companion object {
    private const val PREFIX = "--"

    /** Splits [words] into positional arguments and options. */
    public fun parse(words: List<String>): Arguments {
      val positional = mutableListOf<String>()
      val options = mutableMapOf<String, String>()
      var index = 0
      while (index < words.size) {
        val word = words[index]
        index++
        if (!word.startsWith(PREFIX)) {
          positional += word
          continue
        }
        val body = word.removePrefix(PREFIX)
        val equals = body.indexOf('=')
        if (equals >= 0) {
          options[body.substring(0, equals)] = body.substring(equals + 1)
        } else {
          val value = words.getOrNull(index) ?: throw CliFailure("--$body needs a value")
          index++
          options[body] = value
        }
      }
      return Arguments(positional, options)
    }
  }
}
