package dev.chieppa.ease.codegen.output

import dev.chieppa.ease.codegen.model.DescribedType
import dev.chieppa.ease.codegen.model.SerialField
import dev.chieppa.ease.codegen.model.SerialType
import dev.chieppa.ease.codegen.model.TYPE_KEY
import dev.chieppa.ease.codegen.model.TypeKind
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
 * `description`, `@Range` is `minimum` and `maximum`.
 *
 * Whether a class is written with its serial name under `type` depends on how it is reached. The
 * top-level schema of a class that belongs to a polymorphic base requires `type`, since that is how
 * the world codec writes it. Under `$defs` a class is plain, as a field typed with the class itself
 * holds it; a sealed hierarchy refers to its subtypes through a variant, the serial name plus
 * [JsonSchemaTypes.VARIANT_SUFFIX], that requires `type`.
 */
internal object JsonSchemaSource {
  /** Added to a serial name under `$defs` for the form of a class that requires `type`. */
  private const val DRAFT = "https://json-schema.org/draft/2020-12/schema"
  private const val ID_PREFIX = "urn:ease:"
  private const val DEFS = "\$defs"

  /** The schema of [described], with every type it reaches under `$defs`. */
  fun of(described: DescribedType): JsonObject = buildJsonObject {
    val root = described.type
    val context = SchemaContext(root, described.reachable)
    put("\$schema", DRAFT)
    put("\$id", ID_PREFIX + root.serialName)
    put("title", root.serialName)
    body(root, context, typed = root.discriminated).forEach { (key, value) -> put(key, value) }
    val definitions = definitions(context)
    if (definitions.isNotEmpty())
      putJsonObject(DEFS) { definitions.forEach { (k, v) -> put(k, v) } }
  }

  private fun definitions(context: SchemaContext): Map<String, JsonElement> {
    val root = context.root
    val definitions = linkedMapOf<String, JsonElement>()
    context.reachable.values.forEach { type ->
      if (type.serialName != root.serialName || root.discriminated) {
        definitions[type.serialName] = JsonObject(body(type, context, typed = false))
      }
    }
    variantNames(context).forEach { name ->
      val type = checkNotNull(context.reachable[name])
      definitions[name + JsonSchemaTypes.VARIANT_SUFFIX] =
        JsonObject(body(type, context, typed = true))
    }
    return definitions
  }

  /** The classes a sealed hierarchy in the schema refers to, other than the top-level type. */
  private fun variantNames(context: SchemaContext): List<String> =
    context.reachable.values
      .filter { it.kind == TypeKind.SEALED }
      .flatMap { it.subtypes }
      .distinct()
      .filter { it != context.root.serialName && context.reachable[it]?.kind == TypeKind.CLASS }

  private fun body(
    type: SerialType,
    context: SchemaContext,
    typed: Boolean,
  ): Map<String, JsonElement> =
    when (type.kind) {
      TypeKind.CLASS -> classBody(type, context, typed)
      TypeKind.ENUM ->
        described(type) {
          put("type", "string")
          putJsonArray("enum") { type.values.forEach { add(JsonPrimitive(it)) } }
        }
      TypeKind.VALUE -> valueBody(type, context)
      TypeKind.SEALED -> sealedBody(type, context)
    }

  /** A hierarchy with no serializable subtype holds no value at all: `not` of the any-schema. */
  private fun sealedBody(type: SerialType, context: SchemaContext): Map<String, JsonElement> =
    described(type) {
      if (type.subtypes.isEmpty()) {
        putJsonObject("not") {}
      } else {
        putJsonArray("oneOf") { type.subtypes.forEach { add(subtypeReference(it, context)) } }
      }
    }

  private fun subtypeReference(name: String, context: SchemaContext): JsonObject {
    val isClass = context.reachable[name]?.kind == TypeKind.CLASS
    return if (isClass) JsonSchemaTypes.reference(name, context, typed = true)
    else JsonSchemaTypes.reference(name, context)
  }

  private fun classBody(type: SerialType, context: SchemaContext, typed: Boolean) =
    described(type) {
      put("type", "object")
      putJsonObject("properties") {
        if (typed) putJsonObject(TYPE_KEY) { put("const", type.serialName) }
        type.fields.forEach { put(it.name, fieldSchema(it, context)) }
      }
      val required =
        listOfNotNull(TYPE_KEY.takeIf { typed }) +
          type.fields.filterNot { it.optional }.map { it.name }
      putJsonArray("required") { required.forEach { add(JsonPrimitive(it)) } }
      put("additionalProperties", false)
    }

  private fun valueBody(type: SerialType, context: SchemaContext): Map<String, JsonElement> {
    val field = type.fields.single()
    val inner = JsonSchemaTypes.of(field.type, field.range, context)
    return described(type) { inner.forEach { (key, value) -> put(key, value) } }
  }

  private fun described(type: SerialType, build: JsonObjectBuilder.() -> Unit) = buildJsonObject {
    type.doc?.let { put("description", it) }
    build()
  }

  private fun fieldSchema(field: SerialField, context: SchemaContext): JsonObject =
    buildJsonObject {
      field.doc?.let { put("description", it) }
      JsonSchemaTypes.of(field.type, field.range, context).forEach { (key, value) ->
        put(key, value)
      }
    }
}
