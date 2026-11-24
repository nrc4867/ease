package dev.chieppa.ease.serialization.yaml

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class CanonicalYamlTest {
  private fun tree() =
    JsonObject(
      mapOf(
        "zeta" to JsonPrimitive(1),
        "alpha" to JsonPrimitive("hello"),
        "list" to JsonArray(listOf(JsonPrimitive(0.5), JsonPrimitive("x y"))),
        "nested" to JsonObject(mapOf("b" to JsonNull, "a" to JsonPrimitive(true))),
        "empty" to JsonObject(emptyMap()),
      )
    )

  @Test
  fun `keys are sorted and the layout is fixed`() {
    val expected =
      listOf(
        "alpha: hello",
        "empty: {}",
        "list:",
        "  - 0.5",
        "  - \"x y\"",
        "nested:",
        "  a: true",
        "  b: null",
        "zeta: 1",
        "",
      )

    assertEquals(expected.joinToString("\n"), CanonicalYaml.write(tree()))
  }

  @Test
  fun `the same content always writes the same bytes`() {
    val reordered = JsonObject(tree().entries.reversed().associate { it.key to it.value })

    assertEquals(CanonicalYaml.write(tree()), CanonicalYaml.write(reordered))
  }

  @Test
  fun `writing then reading gives the tree back`() {
    assertEquals(tree(), CanonicalYaml.read(CanonicalYaml.write(tree())))
  }

  @Test
  fun `strings that look like other values stay strings`() {
    val strings = listOf("true", "null", "123", "1.5", "2024-01-01", "", " pad", "a: b", "yes", "~")
    val tree =
      JsonObject(strings.withIndex().associate { "k${it.index}" to JsonPrimitive(it.value) })

    val back = CanonicalYaml.read(CanonicalYaml.write(tree)) as JsonObject

    assertEquals(strings, back.values.map { (it as JsonPrimitive).content })
    assertTrue(back.values.all { (it as JsonPrimitive).isString })
  }

  @Test
  fun `control characters and unicode survive`() {
    val tree = JsonObject(mapOf("text" to JsonPrimitive("line\nTab\t\"quote\" \u0001 é中")))

    assertEquals(tree, CanonicalYaml.read(CanonicalYaml.write(tree)))
  }

  @Test
  fun `numbers keep their shortest form`() {
    val tree = JsonObject(mapOf("a" to JsonPrimitive(0.1), "b" to JsonPrimitive(1.0e10)))

    assertEquals("a: 0.1\nb: 1.0E10\n", CanonicalYaml.write(tree))
  }

  @Test
  fun `typed values go through their serializer`() {
    val value = Sample(name = "crate", count = 3)

    val text = CanonicalYaml.encode(Sample.serializer(), value)

    assertEquals("count: 3\nname: crate\n", text)
    assertEquals(value, CanonicalYaml.decode(Sample.serializer(), text))
  }

  @Test
  fun `text that is not YAML is refused`() {
    assertFailsWith<YamlFormatException> { CanonicalYaml.read("a: [unclosed") }
  }

  @Test
  fun `a key written twice is refused`() {
    assertFailsWith<YamlFormatException> { CanonicalYaml.read("a: 1\na: 2\n") }
  }

  @Serializable private data class Sample(val name: String, val count: Int)
}
