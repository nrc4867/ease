package dev.chieppa.ease.agent.catalog

import dev.chieppa.ease.agent.describe.SchemaCatalog
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** The schemas the build generated for this module's types, read as `describe` reads them. */
class SchemaCatalogTest {
  private val catalog = SchemaCatalog.load(SchemaCatalogTest::class.java.classLoader)

  private fun schema(name: String): JsonObject = checkNotNull(catalog.schemaOf(name))

  private fun JsonObject.child(key: String): JsonObject = getValue(key).jsonObject

  private fun JsonObject.property(name: String): JsonObject = child("properties").child(name)

  private fun strings(vararg values: String): JsonArray = buildJsonArray {
    values.forEach { add(JsonPrimitive(it)) }
  }

  @Test
  fun `a class has its properties by serial name, a doc, and requires what has no default`() {
    val crate = schema("test.Crate")

    assertEquals("A crate.", crate.getValue("description").jsonPrimitive.content)
    assertEquals(
      setOf("id", "label", "tags", "weights", "mood", "weather", "lid", "height"),
      crate.child("properties").keys,
    )
    assertEquals(
      strings("id", "label", "tags", "weights", "mood", "weather"),
      crate.getValue("required"),
    )
    assertEquals(JsonPrimitive(false), crate.getValue("additionalProperties"))
  }

  @Test
  fun `a transient property is left out`() {
    assertTrue("scratch" !in schema("test.Crate").child("properties").keys)
  }

  @Test
  fun `a nullable field with a range gives the range on its number and a null alternative`() {
    val height = schema("test.Crate").property("height")

    assertEquals("How tall it is.", height.getValue("description").jsonPrimitive.content)
    val alternatives = height.getValue("anyOf").jsonArray
    val number = alternatives[0].jsonObject
    assertEquals("number", number.getValue("type").jsonPrimitive.content)
    assertEquals("0", number.getValue("minimum").jsonPrimitive.content)
    assertEquals("9.5", number.getValue("maximum").jsonPrimitive.content)
    assertEquals("null", alternatives[1].jsonObject.getValue("type").jsonPrimitive.content)
  }

  @Test
  fun `lists and maps describe their elements`() {
    val crate = schema("test.Crate")

    val tags = crate.property("tags")
    assertEquals("array", tags.getValue("type").jsonPrimitive.content)
    assertEquals("string", tags.child("items").getValue("type").jsonPrimitive.content)
    val weights = crate.property("weights")
    assertEquals(
      "integer",
      weights.child("additionalProperties").getValue("type").jsonPrimitive.content,
    )
  }

  @Test
  fun `enums, value classes and sealed hierarchies are described under defs`() {
    val crate = schema("test.Crate")
    val defs = crate.child("\$defs")

    assertEquals(
      "#/\$defs/test.Mood",
      crate.property("mood").getValue("\$ref").jsonPrimitive.content,
    )
    assertEquals(strings("CALM", "angry"), defs.child("test.Mood").getValue("enum"))
    assertEquals("integer", defs.child("test.CrateId").getValue("type").jsonPrimitive.content)
    val weather = defs.child("test.Weather").getValue("oneOf").jsonArray
    assertEquals(
      listOf("#/\$defs/test.Rain", "#/\$defs/test.Sun"),
      weather.map { it.jsonObject.getValue("\$ref").jsonPrimitive.content },
    )
  }

  @Test
  fun `a member of a sealed hierarchy carries its serial name as the constant type`() {
    val rain = schema("test.Rain")

    assertEquals(JsonPrimitive("test.Rain"), rain.property("type").getValue("const"))
    assertEquals(strings("type", "millimeters"), rain.getValue("required"))
    assertEquals(strings("type"), schema("test.Sun").getValue("required"))
  }

  @Test
  fun `a type no module generated a schema for is absent`() {
    assertNull(catalog.schemaOf("test.Nothing"))
  }
}
