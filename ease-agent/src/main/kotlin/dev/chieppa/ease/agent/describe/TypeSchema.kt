package dev.chieppa.ease.agent.describe

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

/**
 * A type's schema as JSON, read from the descriptor the serialization plugin generated: its serial
 * `name`, its `kind`, and for a class its `fields`, each with its `name`, whether it is `optional`
 * and its `type`, nested classes spelled out the same way. A type already being spelled out further
 * up names itself only, so recursive types end.
 */
@OptIn(ExperimentalSerializationApi::class)
public object TypeSchema {
  /** The schema of [descriptor]. */
  public fun of(descriptor: SerialDescriptor): JsonObject = schema(descriptor, emptySet())

  private fun schema(descriptor: SerialDescriptor, enclosing: Set<String>): JsonObject =
    buildJsonObject {
      put(NAME, descriptor.serialName.removeSuffix(NULLABLE_MARK))
      put(KIND, kindName(descriptor.kind))
      if (descriptor.isNullable) put(NULLABLE, true)
      if (descriptor.serialName in enclosing) return@buildJsonObject
      val inside = enclosing + descriptor.serialName
      when (descriptor.kind) {
        StructureKind.CLASS,
        StructureKind.OBJECT -> fields(descriptor, inside)
        StructureKind.LIST -> put(ELEMENT, schema(descriptor.getElementDescriptor(0), inside))
        StructureKind.MAP -> {
          put(KEY, schema(descriptor.getElementDescriptor(0), inside))
          put(VALUE, schema(descriptor.getElementDescriptor(1), inside))
        }
        SerialKind.ENUM ->
          putJsonArray(VALUES) {
            repeat(descriptor.elementsCount) { add(descriptor.getElementName(it)) }
          }
        else ->
          if (descriptor.isInline) put(VALUE, schema(descriptor.getElementDescriptor(0), inside))
      }
    }

  private fun JsonObjectBuilder.fields(descriptor: SerialDescriptor, inside: Set<String>) {
    putJsonArray(FIELDS) {
      repeat(descriptor.elementsCount) { index ->
        add(
          buildJsonObject {
            put(NAME, descriptor.getElementName(index))
            if (descriptor.isElementOptional(index)) put(OPTIONAL, true)
            put(TYPE, schema(descriptor.getElementDescriptor(index), inside))
          }
        )
      }
    }
  }

  private fun kindName(kind: SerialKind): String =
    when (kind) {
      PrimitiveKind.BOOLEAN -> "boolean"
      PrimitiveKind.BYTE,
      PrimitiveKind.SHORT,
      PrimitiveKind.INT,
      PrimitiveKind.LONG -> "integer"
      PrimitiveKind.FLOAT,
      PrimitiveKind.DOUBLE -> "number"
      PrimitiveKind.CHAR,
      PrimitiveKind.STRING -> "string"
      StructureKind.CLASS -> "class"
      StructureKind.OBJECT -> "object"
      StructureKind.LIST -> "list"
      StructureKind.MAP -> "map"
      SerialKind.ENUM -> "enum"
      PolymorphicKind.OPEN,
      PolymorphicKind.SEALED -> "polymorphic"
      else -> "contextual"
    }

  private const val NAME = "name"
  private const val KIND = "kind"
  private const val NULLABLE = "nullable"
  private const val NULLABLE_MARK = "?"
  private const val FIELDS = "fields"
  private const val OPTIONAL = "optional"
  private const val TYPE = "type"
  private const val ELEMENT = "element"
  private const val KEY = "key"
  private const val VALUE = "value"
  private const val VALUES = "values"
}
