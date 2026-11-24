package dev.chieppa.ease.codegen

import dev.chieppa.ease.codegen.model.DescribedType
import dev.chieppa.ease.codegen.model.PrimitiveKind
import dev.chieppa.ease.codegen.model.SerialField
import dev.chieppa.ease.codegen.model.SerialType
import dev.chieppa.ease.codegen.model.TypeKind
import dev.chieppa.ease.codegen.model.TypeRef
import dev.chieppa.ease.codegen.model.ValueRange
import dev.chieppa.ease.codegen.output.JsonSchemaSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** The JSON Schema a type becomes, on types built in code. */
class JsonSchemaSourceTest {
  private val number = TypeRef.Primitive(PrimitiveKind.NUMBER)

  private fun schema(root: SerialType, vararg others: SerialType): JsonObject =
    JsonSchemaSource.of(DescribedType(root, (listOf(root) + others).associateBy { it.serialName }))

  private fun classOf(
    name: String,
    vararg fields: SerialField,
    doc: String? = null,
    discriminated: Boolean = false,
  ) = SerialType(name, TypeKind.CLASS, doc, fields.toList(), discriminated = discriminated)

  private fun JsonObject.child(key: String) = getValue(key).jsonObject

  private fun JsonObject.text(key: String) = getValue(key).jsonPrimitive.content

  private fun JsonObject.strings(key: String) =
    getValue(key).jsonArray.map { it.jsonPrimitive.content }

  @Test
  fun `a schema names its draft, its id and its title`() {
    val schema = schema(classOf("game.Lamp"))

    assertEquals("https://json-schema.org/draft/2020-12/schema", schema.text("\$schema"))
    assertEquals("game.Lamp", schema.text("title"))
    assertEquals("urn:ease:game.Lamp", schema.text("\$id"))
  }

  @Test
  fun `a doc is a description and a range is a minimum and a maximum`() {
    val level = SerialField("level", number, "How bright.", ValueRange(-1.0, 0.25))
    val schema = schema(classOf("game.Lamp", level, doc = "A lamp."))

    assertEquals("A lamp.", schema.text("description"))
    val property = schema.child("properties").child("level")
    assertEquals("How bright.", property.text("description"))
    assertEquals(JsonPrimitive(-1), property.getValue("minimum"))
    assertEquals(JsonPrimitive(0.25), property.getValue("maximum"))
  }

  @Test
  fun `a range end beyond what a Long holds stays the double it is`() {
    val size = SerialField("size", number, range = ValueRange(0.0, 1e20))

    val property = schema(classOf("game.Big", size)).child("properties").child("size")

    assertEquals(JsonPrimitive(1e20), property.getValue("maximum"))
  }

  @Test
  fun `a type that refers to itself points at the schema root`() {
    val next = SerialField("next", TypeRef.Nullable(TypeRef.Named("game.Node")))

    val property = schema(classOf("game.Node", next)).child("properties").child("next")

    val reference = property.getValue("anyOf").jsonArray[0].jsonObject
    assertEquals("#", reference.text("\$ref"))
  }

  @Test
  fun `a polymorphic base in a field is an object that names its type`() {
    val holder = classOf("game.Holder", SerialField("part", TypeRef.Open("Part")))

    val part = schema(holder).child("properties").child("part")

    assertEquals("object", part.text("type"))
    assertEquals(listOf("type"), part.strings("required"))
  }

  @Test
  fun `types a schema reaches are defined once under defs, the root left out`() {
    val id = SerialType("game.Id", TypeKind.VALUE, "An id.", listOf(SerialField("value", number)))
    val lamp = classOf("game.Lamp", SerialField("id", TypeRef.Named("game.Id")))

    val defs = schema(lamp, id).child("\$defs")

    assertEquals(setOf("game.Id"), defs.keys)
    assertEquals("An id.", defs.child("game.Id").text("description"))
  }

  @Test
  fun `the top-level schema of a member of a polymorphic base requires type`() {
    val rain = classOf("game.Rain", SerialField("mm", number), discriminated = true)

    val schema = schema(rain)

    assertEquals(JsonPrimitive("game.Rain"), schema.child("properties").child("type")["const"])
    assertEquals(listOf("type", "mm"), schema.strings("required"))
  }

  @Test
  fun `a field typed with the member itself holds it without type, a sealed base with it`() {
    val rain = classOf("game.Rain", SerialField("mm", number), discriminated = true)
    val weather = SerialType("game.Weather", TypeKind.SEALED, subtypes = listOf("game.Rain"))
    val holder =
      classOf(
        "game.Holder",
        SerialField("rain", TypeRef.Named("game.Rain")),
        SerialField("weather", TypeRef.Named("game.Weather")),
      )

    val schema = schema(holder, rain, weather)

    val defs = schema.child("\$defs")
    val properties = schema.child("properties")
    assertEquals("#/\$defs/game.Rain", properties.child("rain").text("\$ref"))
    assertEquals(listOf("mm"), defs.child("game.Rain").strings("required"))
    val variant = defs.child("game.Rain:typed")
    assertEquals(listOf("type", "mm"), variant.strings("required"))
    val oneOf = defs.child("game.Weather").getValue("oneOf").jsonArray
    assertEquals(listOf("#/\$defs/game.Rain:typed"), oneOf.map { it.jsonObject.text("\$ref") })
  }

  @Test
  fun `a sealed hierarchy with no serializable subtype accepts nothing`() {
    val schema = schema(SerialType("game.Empty", TypeKind.SEALED))

    assertFalse("oneOf" in schema)
    assertEquals(JsonObject(emptyMap()), schema.child("not"))
  }
}
