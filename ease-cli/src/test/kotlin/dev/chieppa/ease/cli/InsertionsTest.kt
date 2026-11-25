package dev.chieppa.ease.cli

import dev.chieppa.ease.cli.template.Insertion
import dev.chieppa.ease.cli.template.Insertions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class InsertionsTest {
  private val file =
    listOf("package a", "", "import b.B", "import d.D", "", "class A {", "  // mark", "}")
      .joinToString("\n")

  @Test
  fun `text goes above the marker at its indent and imports into sorted places`() {
    val insertion = Insertion("A.kt", "// mark", "run()", listOf("c.C", "e.E"))

    val expected =
      listOf(
          "package a",
          "",
          "import b.B",
          "import c.C",
          "import d.D",
          "import e.E",
          "",
          "class A {",
          "  run()",
          "  // mark",
          "}",
        )
        .joinToString("\n")
    assertEquals(expected, Insertions.apply(file, insertion, "A.kt"))
  }

  @Test
  fun `a file with no imports gets them after the package line`() {
    val bare = "package a\n\nclass A {\n  // mark\n}"

    val result =
      Insertions.apply(bare, Insertion("A.kt", "// mark", "run()", listOf("b.B")), "A.kt")

    assertEquals("package a\n\nimport b.B\n\nclass A {\n  run()\n  // mark\n}", result)
  }

  @Test
  fun `applying twice changes nothing the second time`() {
    val insertion = Insertion("A.kt", "// mark", "run()", listOf("c.C"))
    val once = Insertions.apply(file, insertion, "A.kt")

    assertEquals(once, Insertions.apply(once, insertion, "A.kt"))
  }

  @Test
  fun `a missing marker is named`() {
    val failure =
      assertFailsWith<CliFailure> {
        Insertions.apply("class A", Insertion("A.kt", "// mark", "x"), "A.kt")
      }

    assertEquals("A.kt has no // mark line to add to", failure.message)
  }
}
