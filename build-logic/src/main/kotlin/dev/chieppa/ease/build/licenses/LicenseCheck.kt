package dev.chieppa.ease.build.licenses

/**
 * Decides the license of each shipped module and whether it may ship. An override wins over what
 * the POM declares. Several declared licenses are a choice, so they join with `OR`.
 */
object LicenseCheck {
  fun resolve(
    modules: List<ModuleLicenses>,
    overrides: List<LicenseOverride>,
  ): List<ShippedLicense> {
    val byModule = overrides.associateBy { it.module }
    return modules.sortedWith(compareBy({ it.module }, { it.version })).map { module ->
      resolve(module, byModule[module.module])
    }
  }

  /** Overrides naming a module that does not ship, which would otherwise go unnoticed. */
  fun unusedOverrides(
    modules: List<ModuleLicenses>,
    overrides: List<LicenseOverride>,
  ): List<String> {
    val shipped = modules.map { it.module }.toSet()
    return overrides
      .filter { it.module !in shipped }
      .map { "${it.module}: overridden but not shipped; remove its override" }
  }

  private fun resolve(module: ModuleLicenses, override: LicenseOverride?): ShippedLicense {
    val license = override?.license ?: declaredLicense(module)
    val unknown = module.declared.filter { LicenseNames.spdxOf(it.name, it.url) == null }
    val problem =
      when {
        license != null && LicensePolicy.allows(license) -> null
        override == null && module.declared.isEmpty() ->
          "declares no license; add an override with its license and why"
        override == null && unknown.isNotEmpty() ->
          "declares a license that is not recognized (${unknown.joinToString { it.name }}); " +
            "add its name to LicenseNames or an override"
        else -> "is licensed $license, which may not ship"
      }
    return ShippedLicense(module.module, module.version, license, problem)
  }

  private fun declaredLicense(module: ModuleLicenses): String? =
    module.declared
      .mapNotNull { LicenseNames.spdxOf(it.name, it.url) }
      .distinct()
      .takeIf { it.isNotEmpty() }
      ?.joinToString(" OR ")
}
