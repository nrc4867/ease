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

  /** Whether the flag `--[name]` was given. */
  public fun flag(name: String): Boolean = options[name] == FLAG_ON

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
    private const val FLAG_ON = "true"

    /** Splits [words] into positional arguments and options; the names in [flags] take no value. */
    public fun parse(words: List<String>, flags: Set<String> = Options.FLAGS): Arguments {
      val positional = mutableListOf<String>()
      val options = mutableMapOf<String, String>()
      var index = 0
      while (index < words.size) {
        val word = words[index++]
        if (word.startsWith(PREFIX)) {
          index = readOption(words, index, word.removePrefix(PREFIX), flags, options)
        } else {
          positional += word
        }
      }
      return Arguments(positional, options)
    }

    /** Reads the option [body], whose value may be the word at [next]; returns the next word. */
    private fun readOption(
      words: List<String>,
      next: Int,
      body: String,
      flags: Set<String>,
      options: MutableMap<String, String>,
    ): Int {
      val equals = body.indexOf('=')
      return when {
        body in flags -> {
          options[body] = FLAG_ON
          next
        }
        equals >= 0 -> {
          options[body.substring(0, equals)] = body.substring(equals + 1)
          next
        }
        else -> {
          options[body] = words.getOrNull(next) ?: throw CliFailure("--$body needs a value")
          next + 1
        }
      }
    }
  }
}
