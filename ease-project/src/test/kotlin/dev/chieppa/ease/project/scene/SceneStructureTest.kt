package dev.chieppa.ease.project.scene

import dev.chieppa.ease.project.TestProject.Companion.node
import dev.chieppa.ease.scene.document.SceneDocument
import dev.chieppa.ease.scene.node.LocalId
import dev.chieppa.ease.scene.node.OrderKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SceneStructureTest {
  @Test
  fun `respacing keeps the first sibling key and orders the others after it`() {
    val key = OrderKey.FIRST
    val last = OrderKey.after(key)
    val document =
      SceneDocument(
        nodes =
          listOf(
            node(1, "Hall"),
            node(2, "A", parent = 1, order = key),
            node(3, "B", parent = 1, order = key),
            node(4, "C", parent = 1, order = key),
            node(5, "D", parent = 1, order = last),
          )
      )

    val keys = SceneStructure.respaced(document)

    assertEquals(setOf(LocalId(3), LocalId(4)), keys.keys)
    val second = keys.getValue(LocalId(3))
    val third = keys.getValue(LocalId(4))
    assertTrue(key < second && second < third && third < last)
  }

  @Test
  fun `a loop of parents is found once with its members`() {
    val document =
      SceneDocument(
        nodes =
          listOf(
            node(1, "Top"),
            node(2, "A", parent = 3),
            node(3, "B", parent = 2),
            node(4, "C", parent = 2),
          )
      )

    assertEquals(listOf(listOf(LocalId(2), LocalId(3))), SceneStructure.parentCycles(document))
  }
}
