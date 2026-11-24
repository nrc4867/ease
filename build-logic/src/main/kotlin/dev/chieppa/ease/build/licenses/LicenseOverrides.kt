package dev.chieppa.ease.build.licenses

/**
 * Reads the overrides file: one override per line as `group:name | license | reason`, where the
 * license is an SPDX-like expression. Blank lines and lines starting with `#` are skipped.
 */
object LicenseOverrides {
  private const val FIELDS = 3

  fun parse(lines: List<String>): List<LicenseOverride> =
    lines
      .map(String::trim)
      .filter { it.isNotEmpty() && !it.startsWith("#") }
      .map { line ->
        val fields = line.split("|").map(String::trim)
        require(fields.size == FIELDS && fields.none(String::isEmpty)) {
          "License override must be `group:name | license | reason`: $line"
        }
        LicenseOverride(fields[0], fields[1], fields[2])
      }
}
