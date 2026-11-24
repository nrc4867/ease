package dev.chieppa.ease.codegen.output

import dev.chieppa.ease.codegen.model.PrimitiveKind
import dev.chieppa.ease.codegen.model.TYPE_KEY
import dev.chieppa.ease.codegen.model.TypeRef
import dev.chieppa.ease.codegen.model.ValueRange
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/** How a field's [TypeRef] is written as a JSON Schema, and how one type refers to another. */
internal object JsonSchemaTypes {
  /** Added to a serial name under `$defs` for the form of a class that requires `type`. */
  const val VARIANT_SUFFIX = ":typed"

  private const val DEFS_REF = "#/\$defs/"
  private const val ROOT_REF = "#"

  /** The schema of [type], with [range] on a number. */
  fun of(type: TypeRef, range: ValueRange?, context: SchemaContext): JsonObject =
    when (type) {
      is TypeRef.Primitive -> primitive(type.kind, range)
      is TypeRef.Named -> reference(type.serialName, context)
      is TypeRef.ListOf ->
        buildJsonObject {
          put("type", "array")
          put("items", of(type.element, null, context))
        }
      is TypeRef.MapOf ->
        buildJsonObject {
          put("type", "object")
          put("additionalProperties", of(type.value, null, context))
        }
      is TypeRef.Nullable ->
        buildJsonObject {
          putJsonArray("anyOf") {
            add(of(type.inner, range, context))
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

  /**
   * A reference to [serialName]: the plain form, or with [typed] the form that requires `type`. The
   * top-level schema is the typed form when its type is discriminated and the plain form otherwise,
   * and `#` points at it.
   */
  fun reference(serialName: String, context: SchemaContext, typed: Boolean = false): JsonObject {
    val root = context.root
    val atRoot = serialName == root.serialName && typed == root.discriminated
    val target =
      when {
        atRoot -> ROOT_REF
        typed && serialName == root.serialName -> ROOT_REF
        typed -> DEFS_REF + serialName + VARIANT_SUFFIX
        else -> DEFS_REF + serialName
      }
    return buildJsonObject { put("\$ref", target) }
  }

  private fun open(type: TypeRef.Open): JsonObject = buildJsonObject {
    put("type", "object")
    put("description", "Any ${type.base} the world registers, named by its serial name under type.")
    putJsonObject("properties") { putJsonObject(TYPE_KEY) { put("type", "string") } }
    putJsonArray("required") { add(JsonPrimitive(TYPE_KEY)) }
  }

  private fun number(value: Double): JsonPrimitive =
    NumberText.whole(value)?.let { JsonPrimitive(it) } ?: JsonPrimitive(value)
}
