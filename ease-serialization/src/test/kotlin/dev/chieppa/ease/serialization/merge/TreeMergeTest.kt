package dev.chieppa.ease.serialization.merge

import dev.chieppa.ease.serialization.yaml.CanonicalYaml
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TreeMergeTest {
  private fun tree(text: String) = CanonicalYaml.read(text.trimIndent() + "\n")

  private val base =
    tree(
      """
    a: 1
    b:
      x: 1
      y: 2
    list: [1, 2]
  """
    )

  @Test
  fun `changes to different keys, neighbours included, merge`() {
    val ours =
      tree(
        """
      a: 5
      b:
        x: 1
        y: 2
      list: [1, 2]
    """
      )
    val theirs =
      tree(
        """
      a: 1
      b:
        x: 1
        y: 2
        z: 3
      list: [1, 2]
    """
      )

    val outcome = TreeMerge.merge(base, ours, theirs)

    assertTrue(outcome.clean)
    assertEquals(
      tree(
        """
        a: 5
        b:
          x: 1
          y: 2
          z: 3
        list: [1, 2]
      """
      ),
      outcome.tree,
    )
  }

  @Test
  fun `two additions of different keys at one spot merge`() {
    val ours = tree("a: 1\nb: {x: 1, y: 2, p: 1}\nlist: [1, 2]")
    val theirs = tree("a: 1\nb: {x: 1, y: 2, q: 1}\nlist: [1, 2]")

    val outcome = TreeMerge.merge(base, ours, theirs)

    assertTrue(outcome.clean)
    assertEquals(tree("a: 1\nb: {x: 1, y: 2, p: 1, q: 1}\nlist: [1, 2]"), outcome.tree)
  }

  @Test
  fun `the same change on both sides is kept and a one-sided delete is applied`() {
    val ours = tree("a: 2\nb: {x: 1}\nlist: [1, 2]")
    val theirs = tree("a: 2\nb: {x: 1, y: 2}\nlist: [1, 2]")

    val outcome = TreeMerge.merge(base, ours, theirs)

    assertTrue(outcome.clean)
    assertEquals(tree("a: 2\nb: {x: 1}\nlist: [1, 2]"), outcome.tree)
  }

  @Test
  fun `different changes to one value conflict and name its path`() {
    val ours = tree("a: 1\nb: {x: 7, y: 2}\nlist: [1, 2]")
    val theirs = tree("a: 1\nb: {x: 8, y: 2}\nlist: [1, 2]")

    val outcome = TreeMerge.merge(base, ours, theirs)

    assertEquals(listOf("b/x"), outcome.conflicts)
  }

  @Test
  fun `a delete against a change and two different list edits conflict`() {
    val ours = tree("a: 1\nb: {x: 1, y: 9}\nlist: [1, 3]")
    val theirs = tree("a: 1\nb: {x: 1}\nlist: [1, 4]")

    val outcome = TreeMerge.merge(base, ours, theirs)

    assertEquals(listOf("b/y", "list"), outcome.conflicts.sorted())
  }
}
