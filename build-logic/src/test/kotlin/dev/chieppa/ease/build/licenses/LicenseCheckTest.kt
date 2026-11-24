package dev.chieppa.ease.build.licenses

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class LicenseCheckTest {
  private fun module(vararg declared: DeclaredLicense) =
    ModuleLicenses("dev.example:lib", "1.0", declared.toList())

  private fun only(vararg declared: DeclaredLicense, overrides: List<LicenseOverride> = listOf()) =
    LicenseCheck.resolve(listOf(module(*declared)), overrides).single()

  @Test
  fun `a permissive license ships`() {
    val shipped = only(DeclaredLicense("The Apache Software License, Version 2.0", ""))
    assertEquals("Apache-2.0", shipped.license)
    assertNull(shipped.problem)
  }

  @Test
  fun `a copyleft license does not ship`() {
    val shipped = only(DeclaredLicense("GNU Lesser General Public License, version 2.1", ""))
    assertEquals("LGPL", shipped.license)
    assertNotNull(shipped.problem)
  }

  @Test
  fun `several declared licenses are a choice`() {
    val shipped = only(DeclaredLicense("LGPL-2.1", ""), DeclaredLicense("Apache License 2.0", ""))
    assertNull(shipped.problem)
  }

  @Test
  fun `a module with no license needs an override`() {
    assertNotNull(only().problem)
    val overridden = only(overrides = listOf(LicenseOverride("dev.example:lib", "MIT", "why")))
    assertEquals("MIT", overridden.license)
    assertNull(overridden.problem)
  }

  @Test
  fun `an override to a refused license does not ship`() {
    val override = LicenseOverride("dev.example:lib", "MPL-2.0", "why")
    assertNotNull(only(DeclaredLicense("MIT", ""), overrides = listOf(override)).problem)
  }

  @Test
  fun `an unrecognized license is a problem`() {
    assertNotNull(only(DeclaredLicense("Some Custom Terms", "https://example.com/terms")).problem)
  }

  @Test
  fun `an override for a module that does not ship is reported`() {
    val overrides = listOf(LicenseOverride("dev.example:gone", "MIT", "why"))
    assertEquals(1, LicenseCheck.unusedOverrides(listOf(module()), overrides).size)
  }
}
