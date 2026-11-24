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

  private fun classOf(name: String, vararg fields: SerialField, doc: String? = null) =
    SerialType(name, TypeKind.CLASS, doc, fields.toList())

  private fun JsonObject.child(key: String) = getValue(key).jsonObject

  private fun JsonObject.text(key: String) = getValue(key).jsonPrimitive.content

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
    assertEquals(
      listOf("type"),
      part.getValue("required").jsonArray.map { it.jsonPrimitive.content },
    )
  }

  @Test
  fun `types a schema reaches are defined once under defs, the root left out`() {
    val id = SerialType("game.Id", TypeKind.VALUE, "An id.", listOf(SerialField("value", number)))
    val lamp = classOf("game.Lamp", SerialField("id", TypeRef.Named("game.Id")))

    val defs = schema(lamp, id).child("\$defs")

    assertEquals(setOf("game.Id"), defs.keys)
    assertEquals("An id.", defs.child("game.Id").text("description"))
  }
}
