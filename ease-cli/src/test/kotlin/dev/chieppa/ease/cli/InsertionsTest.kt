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

  @Test
  fun `the same line elsewhere in the file does not count as already added`() {
    val elsewhere = "package a\n\nfun f() {\n  run()\n}\n\nclass A {\n  // mark\n}"

    val result = Insertions.apply(elsewhere, Insertion("A.kt", "// mark", "run()"), "A.kt")

    assertEquals("package a\n\nfun f() {\n  run()\n}\n\nclass A {\n  run()\n  // mark\n}", result)
  }

  @Test
  fun `several lines count as added only when they are all directly above the marker`() {
    val insertion = Insertion("A.kt", "// mark", "one()\ntwo()")
    val split = "class A {\n  one()\n  other()\n  two()\n  // mark\n}"

    val result = Insertions.apply(split, insertion, "A.kt")

    assertEquals("class A {\n  one()\n  other()\n  two()\n  one()\n  two()\n  // mark\n}", result)
  }

  @Test
  fun `a statement stacked above the marker by an earlier insertion is not added again`() {
    val stacked = "class A {\n  a()\n  b()\n  // mark\n}"

    val result = Insertions.apply(stacked, Insertion("A.kt", "// mark", "a()"), "A.kt")

    assertEquals(stacked, result)
  }
}
