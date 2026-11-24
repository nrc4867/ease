package dev.chieppa.ease.agent.tools

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * A tool's arguments as the JSON schema of an object: its [properties] by name, and those
 * [required].
 */
public class ToolInput(
  public val properties: JsonObject = JsonObject(emptyMap()),
  public val required: List<String> = emptyList(),
) {
  /** Builds a [ToolInput] one property at a time. */
  public class Builder internal constructor() {
    private val properties = mutableMapOf<String, JsonObject>()
    private val required = mutableListOf<String>()

    /** A text property, limited to [oneOf] when it is given. */
    public fun string(
      name: String,
      doc: String,
      required: Boolean = false,
      oneOf: List<String> = emptyList(),
    ) {
      add(name, required) {
        put(TYPE, STRING)
        put(DESCRIPTION, doc)
        if (oneOf.isNotEmpty()) putJsonArray(ENUM) { oneOf.forEach { add(JsonPrimitive(it)) } }
      }
    }

    /** A whole-number property from [range]'s start to its end. */
    public fun integer(name: String, doc: String, range: LongRange) {
      add(name, false) {
        put(TYPE, INTEGER)
        put(DESCRIPTION, doc)
        put(MINIMUM, range.first)
        put(MAXIMUM, range.last)
      }
    }

    /** A number property. */
    public fun number(name: String, doc: String) {
      add(name, false) {
        put(TYPE, NUMBER)
        put(DESCRIPTION, doc)
      }
    }

    /** A list of whole numbers. */
    public fun integers(name: String, doc: String) {
      add(name, false) {
        put(TYPE, ARRAY)
        put(DESCRIPTION, doc)
        putJsonObject(ITEMS) { put(TYPE, INTEGER) }
      }
    }

    private fun add(name: String, isRequired: Boolean, schema: JsonObjectBuilder.() -> Unit) {
      properties[name] = buildJsonObject(schema)
      if (isRequired) required += name
    }

    internal fun build(): ToolInput = ToolInput(JsonObject(properties), required.toList())
  }

  public companion object {
    private const val TYPE = "type"
    private const val DESCRIPTION = "description"
    private const val ENUM = "enum"
    private const val ITEMS = "items"
    private const val MINIMUM = "minimum"
    private const val MAXIMUM = "maximum"
    private const val STRING = "string"
    private const val INTEGER = "integer"
    private const val NUMBER = "number"
    private const val ARRAY = "array"

    /** The input [build] describes. */
    public fun of(build: Builder.() -> Unit): ToolInput = Builder().apply(build).build()
  }
}
