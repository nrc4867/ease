package dev.chieppa.ease.codegen.output

import dev.chieppa.ease.codegen.model.DescribedType
import dev.chieppa.ease.codegen.model.PrimitiveKind
import dev.chieppa.ease.codegen.model.SerialField
import dev.chieppa.ease.codegen.model.SerialType
import dev.chieppa.ease.codegen.model.TYPE_KEY
import dev.chieppa.ease.codegen.model.TypeKind
import dev.chieppa.ease.codegen.model.TypeRef
import dev.chieppa.ease.codegen.model.ValueRange
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * The JSON Schema (draft 2020-12) of a described type. The schema is self-contained: the type
 * itself at the top and every type reachable from it under `$defs`, by serial name. `@Doc` is
 * `description`, `@Range` is `minimum` and `maximum`, and a class that belongs to a polymorphic
 * base carries its serial name as the constant `type` property.
 */
internal object JsonSchemaSource {
  private const val DRAFT = "https://json-schema.org/draft/2020-12/schema"
  private const val ID_PREFIX = "urn:ease:"
  private const val DEFS = "\$defs"
  private const val ROOT_REF = "#"
  private const val DEFS_REF = "#/$DEFS/"

  /** The schema of [described], with every type it reaches under `$defs`. */
  fun of(described: DescribedType): JsonObject = buildJsonObject {
    val root = described.type
    put("\$schema", DRAFT)
    put("\$id", ID_PREFIX + root.serialName)
    put("title", root.serialName)
    body(root, root.serialName).forEach { (key, value) -> put(key, value) }
    val others = described.reachable.values.filter { it.serialName != root.serialName }
    if (others.isNotEmpty()) {
      putJsonObject(DEFS) {
        others.forEach { put(it.serialName, JsonObject(body(it, root.serialName))) }
      }
    }
  }

  private fun body(type: SerialType, root: String): Map<String, JsonElement> =
    when (type.kind) {
      TypeKind.CLASS -> classBody(type, root)
      TypeKind.ENUM ->
        described(type) {
          put("type", "string")
          putJsonArray("enum") { type.values.forEach { add(JsonPrimitive(it)) } }
        }
      TypeKind.VALUE -> valueBody(type, root)
      TypeKind.SEALED ->
        described(type) {
          putJsonArray("oneOf") { type.subtypes.forEach { add(reference(it, root)) } }
        }
    }

  private fun classBody(type: SerialType, root: String): Map<String, JsonElement> =
    described(type) {
      put("type", "object")
      putJsonObject("properties") {
        if (type.discriminated) putJsonObject(TYPE_KEY) { put("const", type.serialName) }
        type.fields.forEach { put(it.name, fieldSchema(it, root)) }
      }
      val required =
        listOfNotNull(TYPE_KEY.takeIf { type.discriminated }) +
          type.fields.filterNot { it.optional }.map { it.name }
      putJsonArray("required") { required.forEach { add(JsonPrimitive(it)) } }
      put("additionalProperties", false)
    }

  private fun valueBody(type: SerialType, root: String): Map<String, JsonElement> {
    val field = type.fields.single()
    val inner = typeSchema(field.type, field.range, root)
    return described(type) { inner.forEach { (key, value) -> put(key, value) } }
  }

  private fun described(type: SerialType, build: JsonObjectBuilder.() -> Unit) = buildJsonObject {
    type.doc?.let { put("description", it) }
    build()
  }

  private fun fieldSchema(field: SerialField, root: String): JsonObject = buildJsonObject {
    field.doc?.let { put("description", it) }
    typeSchema(field.type, field.range, root).forEach { (key, value) -> put(key, value) }
  }

  private fun typeSchema(type: TypeRef, range: ValueRange?, root: String): JsonObject =
    when (type) {
      is TypeRef.Primitive -> primitive(type.kind, range)
      is TypeRef.Named -> reference(type.serialName, root)
      is TypeRef.ListOf ->
        buildJsonObject {
          put("type", "array")
          put("items", typeSchema(type.element, null, root))
        }
      is TypeRef.MapOf ->
        buildJsonObject {
          put("type", "object")
          put("additionalProperties", typeSchema(type.value, null, root))
        }
      is TypeRef.Nullable ->
        buildJsonObject {
          putJsonArray("anyOf") {
            add(typeSchema(type.inner, range, root))
            add(buildJsonObject { put("type", "null") })
          }
        }
      is TypeRef.Open -> open(type)
      TypeRef.Unknown -> JsonObject(emptyMap())
    }

  private fun primitive(kind: PrimitiveKind, range: ValueRange?): JsonObject = buildJsonObject {
    put("type", kind.name.lowercase())
    if (range != null) {
      put("minimum", number(range.min))
      put("maximum", number(range.max))
    }
  }

  private fun reference(serialName: String, root: String): JsonObject = buildJsonObject {
    put("\$ref", if (serialName == root) ROOT_REF else DEFS_REF + serialName)
  }

  private fun open(type: TypeRef.Open): JsonObject = buildJsonObject {
    put("type", "object")
    put("description", "Any ${type.base} the world registers, named by its serial name under type.")
    putJsonObject("properties") { putJsonObject(TYPE_KEY) { put("type", "string") } }
    putJsonArray("required") { add(JsonPrimitive(TYPE_KEY)) }
  }

  /** [value] as a whole number when it is one, so `0.0` reads as `0`. */
  private fun number(value: Double): JsonPrimitive =
    if (value == Math.rint(value) && Math.abs(value) < MAX_EXACT) JsonPrimitive(value.toLong())
    else JsonPrimitive(value)

  /** The largest magnitude below which every whole double is an exact `Long`. */
  private const val MAX_EXACT = 9.0e15
}
