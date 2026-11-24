package dev.chieppa.ease.build.types

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TypeFragmentsTest {
  private fun declared(name: String, body: String) =
    "${TypeFragments.DECLARED_MARKER}$name\ndeclare namespace x {\n  $body\n}\n"

  private fun reached(name: String, body: String) =
    "${TypeFragments.REACHED_MARKER}$name\ndeclare namespace x {\n  $body\n}\n"

  @Test
  fun `a part splits into one block per type, each keeping its marker line and origin`() {
    val part = declared("a.A", "interface A {}") + "\n" + reached("a.B", "interface B {}")

    val blocks = TypeFragments.blocksOf(part)

    assertEquals(listOf("a.A", "a.B"), blocks.map { it.name })
    assertEquals(listOf(true, false), blocks.map { it.declared })
    assertTrue(blocks[0].text.startsWith(TypeFragments.DECLARED_MARKER + "a.A"))
  }

  @Test
  fun `a type several parts declare is kept once, the first part winning`() {
    val engine = declared("ease.NetworkId", "type NetworkId = number;")
    val other = declared("ease.NetworkId", "type NetworkId = string;")

    val joined = TypeFragments.assemble(listOf(engine, other))

    assertEquals(1, Regex("ease.NetworkId").findAll(joined).count())
    assertTrue("number" in joined && "string" !in joined, joined)
  }

  @Test
  fun `the block of the declaring module beats a reached copy, whichever part comes first`() {
    val gameCopy = reached("ease.NetworkId", "type NetworkId = number;")
    val engineCopy = declared("ease.NetworkId", "/** An id. */\n  type NetworkId = number;")

    val gameFirst = TypeFragments.assemble(listOf(gameCopy, engineCopy))
    val engineFirst = TypeFragments.assemble(listOf(engineCopy, gameCopy))

    assertTrue("/** An id. */" in gameFirst, gameFirst)
    assertEquals(gameFirst, engineFirst)
  }

  @Test
  fun `blocks come out in type-name order whatever the order of the parts`() {
    val first = declared("z.Z", "interface Z {}")
    val second = declared("a.A", "interface A {}")

    val joined = TypeFragments.assemble(listOf(first, second))

    assertTrue(joined.indexOf("a.A") < joined.indexOf("z.Z"), joined)
  }
}
