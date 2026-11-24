package dev.chieppa.ease.build.licenses

import java.io.Serializable

/** A license as a POM declares it, in free text. */
data class DeclaredLicense(val name: String, val url: String) : Serializable

/** A shipped external module, `group:name`, at [version], with the licenses its POMs declare. */
data class ModuleLicenses(
  val module: String,
  val version: String,
  val declared: List<DeclaredLicense>,
) : Serializable

/** A license set by hand for [module], because its POM declares none or declares it unclearly. */
data class LicenseOverride(val module: String, val license: String, val reason: String)

/**
 * The license a shipped module is taken to have: [license] is an SPDX-like expression, or null when
 * none could be read. [problem] says why the module may not ship, or is null when it may.
 */
data class ShippedLicense(
  val module: String,
  val version: String,
  val license: String?,
  val problem: String?,
)
