package dev.chieppa.ease.build.types

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TypeFragmentsTest {
  private fun block(name: String, body: String) =
    "${TypeFragments.BLOCK_MARKER}$name\ndeclare namespace x {\n  $body\n}\n"

  @Test
  fun `a part splits into one block per type, each keeping its marker line`() {
    val part = block("a.A", "interface A {}") + "\n" + block("a.B", "interface B {}")

    val blocks = TypeFragments.blocksOf(part)

    assertEquals(listOf("a.A", "a.B"), blocks.map { it.first })
    assertTrue(blocks.all { (name, text) -> text.startsWith(TypeFragments.BLOCK_MARKER + name) })
  }

  @Test
  fun `a type several parts declare is kept once, the first part winning`() {
    val engine = block("ease.NetworkId", "type NetworkId = number;")
    val other = block("ease.NetworkId", "type NetworkId = string;")

    val joined = TypeFragments.assemble(listOf(engine, other))

    assertEquals(1, Regex("ease-type: ease.NetworkId").findAll(joined).count())
    assertTrue("number" in joined && "string" !in joined, joined)
  }

  @Test
  fun `blocks come out in type-name order whatever the order of the parts`() {
    val first = block("z.Z", "interface Z {}")
    val second = block("a.A", "interface A {}")

    val joined = TypeFragments.assemble(listOf(first, second))

    assertTrue(joined.indexOf("a.A") < joined.indexOf("z.Z"), joined)
  }
}
