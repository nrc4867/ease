package dev.chieppa.ease.build.licenses

/**
 * Which licenses may ship. Licenses are SPDX identifiers; public domain, which has none, is
 * [PUBLIC_DOMAIN]. Everything not in [ALLOWED] is refused.
 */
object LicensePolicy {
  const val PUBLIC_DOMAIN = "LicenseRef-Public-Domain"

  val ALLOWED: Set<String> =
    setOf(
      "MIT",
      "MIT-0",
      "BSD-2-Clause",
      "BSD-3-Clause",
      "Apache-2.0",
      "Zlib",
      "ISC",
      "UPL-1.0",
      "Unlicense",
      PUBLIC_DOMAIN,
      "FTL",
    )

  /**
   * Whether [expression] lets the code ship. An expression is SPDX-like: identifiers joined by `OR`
   * (any one may be chosen) and `AND` (all apply), with `AND` binding tighter. Parentheses are
   * ignored, so only flat expressions are read correctly.
   */
  fun allows(expression: String): Boolean =
    terms(expression).any { term -> term.all { it in ALLOWED } }

  /** [expression] as `OR` terms, each the identifiers its `AND` joins. */
  fun terms(expression: String): List<List<String>> =
    expression
      .replace("(", " ")
      .replace(")", " ")
      .split(OR)
      .map { term -> term.split(AND).map(String::trim).filter(String::isNotEmpty) }
      .filter { it.isNotEmpty() }

  private val OR = Regex("""\s+OR\s+""")
  private val AND = Regex("""\s+AND\s+""")
}
