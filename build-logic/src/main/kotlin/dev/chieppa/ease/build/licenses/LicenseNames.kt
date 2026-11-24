package dev.chieppa.ease.build.licenses

/**
 * Turns the free-text license a POM declares into an SPDX identifier. Permissive licenses are
 * matched by their usual names and URLs; copyleft and non-commercial ones by family, so a refusal
 * names what was found. An unmatched license gives null.
 */
object LicenseNames {
  /** The identifier for a POM license with [name] and [url], or null when neither is known. */
  fun spdxOf(name: String, url: String): String? {
    val key = normalize(name)
    return BY_ID[key]
      ?: BY_NAME[key]
      ?: family(key)
      ?: BY_URL.entries.firstOrNull { (fragment, _) -> fragment in url.lowercase() }?.value
  }

  private fun normalize(text: String): String =
    text.lowercase().replace(Regex("""[^a-z0-9.+-]+"""), " ").trim()

  /** The refused families, matched anywhere in the name; the most specific comes first. */
  private fun family(key: String): String? =
    when {
      "affero" in key || key.startsWith("agpl") -> "AGPL"
      "lesser general public" in key || "library general public" in key -> "LGPL"
      key.startsWith("lgpl") -> "LGPL"
      "general public license" in key || key.startsWith("gpl") -> "GPL"
      "mozilla public" in key || key.startsWith("mpl") -> "MPL"
      "eclipse public" in key || key.startsWith("epl") -> "EPL"
      "non commercial" in key || "noncommercial" in key || "nc" in key.split(' ', '-') -> "CC-BY-NC"
      else -> null
    }

  private val BY_ID: Map<String, String> =
    (LicensePolicy.ALLOWED - LicensePolicy.PUBLIC_DOMAIN).associateBy { normalize(it) }

  private val BY_NAME: Map<String, String> =
    mapOf(
        "Apache-2.0" to
          listOf(
            "Apache 2.0",
            "Apache License 2.0",
            "Apache License, Version 2.0",
            "Apache License v2.0",
            "Apache Software License - Version 2.0",
            "The Apache License, Version 2.0",
            "The Apache Software License, Version 2.0",
          ),
        "MIT" to listOf("MIT License", "The MIT License", "The MIT License (MIT)"),
        "MIT-0" to listOf("MIT No Attribution"),
        "BSD-2-Clause" to
          listOf("BSD 2-Clause License", "The BSD 2-Clause License", "Simplified BSD License"),
        "BSD-3-Clause" to
          listOf(
            "BSD 3-Clause License",
            "The BSD 3-Clause License",
            "New BSD License",
            "BSD License 3",
          ),
        "Zlib" to listOf("zlib License", "The zlib License"),
        "ISC" to listOf("ISC License"),
        "UPL-1.0" to listOf("Universal Permissive License, Version 1.0"),
        "Unlicense" to listOf("The Unlicense"),
        LicensePolicy.PUBLIC_DOMAIN to listOf("Public Domain"),
        "FTL" to listOf("FreeType License", "The FreeType Project License"),
      )
      .flatMap { (id, names) -> names.map { normalize(it) to id } }
      .toMap()

  private val BY_URL: Map<String, String> =
    mapOf(
      "apache.org/licenses/license-2.0" to "Apache-2.0",
      "opensource.org/licenses/mit" to "MIT",
      "opensource.org/license/mit" to "MIT",
      "opensource.org/licenses/bsd-3-clause" to "BSD-3-Clause",
      "opensource.org/license/bsd-3-clause" to "BSD-3-Clause",
      "opensource.org/licenses/bsd-2-clause" to "BSD-2-Clause",
      "opensource.org/license/bsd-2-clause" to "BSD-2-Clause",
      "unlicense.org" to "Unlicense",
    )
}
