package dev.chieppa.ease.cli

import dev.chieppa.ease.cli.template.Naming
import dev.chieppa.ease.cli.template.Placeholders
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class NamingAndPlaceholdersTest {
  @Test
  fun `one name gives every form from any spelling`() {
    listOf("Moon Garden", "moon-garden", "moon_garden", "MoonGarden", "moonGarden").forEach {
      assertEquals("MoonGarden", Naming.pascal(it), it)
      assertEquals("moon-garden", Naming.kebab(it), it)
      assertEquals("moongarden", Naming.flat(it), it)
      assertEquals("moon garden", Naming.spaced(it), it)
    }
  }

  @Test
  fun `names and packages that cannot work are refused`() {
    assertFailsWith<CliFailure> { Naming.requireName("2fast") }
    assertFailsWith<CliFailure> { Naming.requireName("a/b") }
    assertFailsWith<CliFailure> { Naming.requirePackage("Com.Example") }
    assertFailsWith<CliFailure> { Naming.requirePackage("a..b") }
    assertEquals("a.b_c", Naming.requirePackage("a.b_c"))
  }

  @Test
  fun `a marker with no value fails by name`() {
    val failure =
      assertFailsWith<CliFailure> { Placeholders(mapOf("a" to "1")).render("{{a}} {{b}}") }

    assertEquals("The template uses {{b}}, which has no value", failure.message)
  }

  @Test
  fun `markers are replaced everywhere and later values win`() {
    val placeholders = Placeholders(mapOf("a" to "1")) + mapOf("a" to "2", "b" to "3")

    assertEquals("2-3-2", placeholders.render("{{a}}-{{b}}-{{a}}"))
  }
}
