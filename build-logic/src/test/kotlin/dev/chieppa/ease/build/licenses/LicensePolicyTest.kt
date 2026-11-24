package dev.chieppa.ease.build.licenses

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LicensePolicyTest {
  @Test
  fun `permissive licenses are allowed and copyleft ones refused`() {
    assertTrue(LicensePolicy.allows("Zlib"))
    assertTrue(LicensePolicy.allows(LicensePolicy.PUBLIC_DOMAIN))
    assertFalse(LicensePolicy.allows("GPL-3.0-only"))
    assertFalse(LicensePolicy.allows("EPL-2.0"))
  }

  @Test
  fun `OR needs one allowed choice and AND needs every part allowed`() {
    assertTrue(LicensePolicy.allows("(MIT OR Apache-2.0)"))
    assertTrue(LicensePolicy.allows("LGPL-2.1-only OR BSD-3-Clause"))
    assertFalse(LicensePolicy.allows("MIT AND LGPL-2.1-only"))
  }

  @Test
  fun `names and URLs map to SPDX identifiers`() {
    assertEquals("Apache-2.0", LicenseNames.spdxOf("The Apache License, Version 2.0", ""))
    assertEquals("MIT", LicenseNames.spdxOf("", "https://opensource.org/license/mit"))
    assertEquals("BSD-3-Clause", LicenseNames.spdxOf("bsd-3-clause", ""))
    assertEquals("AGPL", LicenseNames.spdxOf("GNU Affero General Public License v3", ""))
    assertEquals("LGPL", LicenseNames.spdxOf("GNU Lesser General Public License", ""))
    assertEquals("CC-BY-NC", LicenseNames.spdxOf("CC BY-NC 4.0", ""))
    assertEquals("LGPL", LicenseNames.spdxOf("LGPL-2.1-only", ""))
    assertNull(LicenseNames.spdxOf("Some Custom Terms", ""))
  }

  @Test
  fun `overrides are read one per line`() {
    val overrides =
      LicenseOverrides.parse(listOf("# comment", "", "dev.example:lib | MIT | Its POM is empty."))
    assertEquals(listOf(LicenseOverride("dev.example:lib", "MIT", "Its POM is empty.")), overrides)
  }
}
