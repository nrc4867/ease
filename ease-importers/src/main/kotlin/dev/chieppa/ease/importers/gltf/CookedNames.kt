package dev.chieppa.ease.importers.gltf

/**
 * Hands out the names of one import's cooked assets: a plain file name made from what the source
 * calls the thing (letters, digits, `.`, `-` and `_` kept, anything else `_`), with a number added
 * when the name is taken ignoring case. The same source always gets the same names, since things
 * are named in the order they appear in the file.
 */
internal class CookedNames {
  private val taken = HashSet<String>()

  /** A free name for [base] (or [fallback] when it is blank) with [extension]. */
  fun take(base: String?, fallback: String, extension: String): String {
    val stem = clean(base?.takeIf { it.isNotBlank() } ?: fallback)
    var name = "$stem.$extension"
    var suffix = 2
    while (!taken.add(name.lowercase())) name = "$stem-${suffix++}.$extension"
    return name
  }

  private fun clean(text: String): String {
    val kept = text.map { if (it.isLetterOrDigit() && it.code < ASCII || it in "-_.") it else '_' }
    return kept.joinToString("").trimStart('.').ifEmpty { "_" }
  }

  private companion object {
    const val ASCII = 128
  }
}
