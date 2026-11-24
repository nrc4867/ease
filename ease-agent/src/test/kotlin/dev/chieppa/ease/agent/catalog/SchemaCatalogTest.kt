package dev.chieppa.ease.agent.catalog

import dev.chieppa.ease.agent.describe.SchemaCatalog
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
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

  private fun JsonObject.text(key: String): String = getValue(key).jsonPrimitive.content

  private fun JsonObject.list(key: String): JsonArray = getValue(key).jsonArray

  private fun JsonElement.text(key: String): String = jsonObject.text(key)

  private fun strings(vararg values: String): JsonArray = buildJsonArray {
    values.forEach { add(JsonPrimitive(it)) }
  }

  private val crateProperties =
    setOf("id", "label", "tags", "weights", "mood", "weather", "lid", "height")

  @Test
  fun `a class has its properties by serial name, a doc, and requires what has no default`() {
    val crate = schema("test.Crate")

    assertEquals("A crate.", crate.text("description"))
    assertEquals(crateProperties, crate.child("properties").keys)
    assertEquals(strings("id", "label", "tags", "weights", "mood", "weather"), crate["required"])
    assertEquals(JsonPrimitive(false), crate["additionalProperties"])
  }

  @Test
  fun `a transient property is left out`() {
    assertTrue("scratch" !in schema("test.Crate").child("properties").keys)
  }

  @Test
  fun `a nullable field with a range gives the range on its number and a null alternative`() {
    val height = schema("test.Crate").property("height")

    assertEquals("How tall it is.", height.text("description"))
    val alternatives = height.list("anyOf")
    assertEquals("number", alternatives[0].text("type"))
    assertEquals("0", alternatives[0].text("minimum"))
    assertEquals("9.5", alternatives[0].text("maximum"))
    assertEquals("null", alternatives[1].text("type"))
  }

  @Test
  fun `lists and maps describe their elements`() {
    val crate = schema("test.Crate")

    val tags = crate.property("tags")
    assertEquals("array", tags.text("type"))
    assertEquals("string", tags.child("items").text("type"))
    val weights = crate.property("weights")
    assertEquals("integer", weights.child("additionalProperties").text("type"))
  }

  @Test
  fun `enums, value classes and sealed hierarchies are described under defs`() {
    val crate = schema("test.Crate")
    val defs = crate.child("\$defs")

    assertEquals("#/\$defs/test.Mood", crate.property("mood").text("\$ref"))
    assertEquals(strings("CALM", "angry"), defs.child("test.Mood")["enum"])
    assertEquals("integer", defs.child("test.CrateId").text("type"))
    val weather = defs.child("test.Weather").list("oneOf")
    assertEquals(
      listOf("#/\$defs/test.Rain", "#/\$defs/test.Sun"),
      weather.map { it.text("\$ref") },
    )
  }

  @Test
  fun `a member of a sealed hierarchy carries its serial name as the constant type`() {
    val rain = schema("test.Rain")

    assertEquals(JsonPrimitive("test.Rain"), rain.property("type")["const"])
    assertEquals(strings("type", "millimeters"), rain["required"])
    assertEquals(strings("type"), schema("test.Sun")["required"])
  }

  @Test
  fun `a type no module generated a schema for is absent`() {
    assertNull(catalog.schemaOf("test.Nothing"))
  }
}
