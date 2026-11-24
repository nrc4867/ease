package dev.chieppa.ease.scene.node

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class OrderKeyTest {
  private fun key(text: String) = OrderKey(text)

  @Test
  fun `a key fits between two neighbours by growing a letter`() {
    assertEquals(key("bn"), OrderKey.between(key("b"), key("c")))
    assertEquals(key("an"), OrderKey.between(null, key("b")))
    assertEquals(key("n"), OrderKey.FIRST)
  }

  @Test
  fun `a key fits after the last and before the first, however often`() {
    var last = OrderKey.FIRST
    var first = OrderKey.FIRST
    repeat(200) {
      val after = OrderKey.after(last)
      val before = OrderKey.between(null, first)
      assertTrue(after > last)
      assertTrue(before < first)
      last = after
      first = before
    }
  }

  @Test
  fun `repeated splitting always finds a key strictly between`() {
    val random = Random(7)
    val keys = mutableListOf(OrderKey.FIRST)
    repeat(500) {
      val index = random.nextInt(keys.size + 1)
      val before = keys.getOrNull(index - 1)
      val after = keys.getOrNull(index)
      val key = OrderKey.between(before, after)
      if (before != null) assertTrue(before < key, "$before < $key")
      if (after != null) assertTrue(key < after, "$key < $after")
      keys.add(index, key)
    }
    assertEquals(keys.sorted(), keys)
    assertEquals(keys.size, keys.toSet().size)
  }

  @Test
  fun `keys are letters that never end in the lowest one`() {
    assertFailsWith<IllegalArgumentException> { key("") }
    assertFailsWith<IllegalArgumentException> { key("ba") }
    assertFailsWith<IllegalArgumentException> { key("B") }
    assertFailsWith<IllegalArgumentException> { OrderKey.between(key("c"), key("b")) }
  }

  @Test
  fun `local ids and node keys read back from their text`() {
    val id = LocalId(-2)
    val key = NodeKey.of(listOf(LocalId(1), LocalId(0x2a)), id)

    assertEquals("fffffffffffffffe", id.text)
    assertEquals(id, LocalId.parse(id.text))
    assertEquals("0000000000000001/000000000000002a/fffffffffffffffe", key.text)
    assertEquals(key, NodeKey.parse(key.text))
    assertEquals(listOf(LocalId(1), LocalId(0x2a)), key.placement)
    assertTrue(key.startsWith(NodeKey.of(LocalId(1))))
    assertFailsWith<IllegalArgumentException> { LocalId.parse("12") }
  }
}
