package dev.chieppa.ease.serialization.yaml

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.snakeyaml.engine.v2.api.Load
import org.snakeyaml.engine.v2.api.LoadSettings

/** Reads YAML text into a [JsonElement] tree with snakeyaml-engine's core schema. */
internal object YamlReader {
  fun read(text: String): JsonElement {
    val settings = LoadSettings.builder().setAllowDuplicateKeys(false).build()
    return convert(Load(settings).loadFromString(text))
  }

  private fun convert(value: Any?): JsonElement =
    when (value) {
      null -> JsonNull
      is Boolean -> JsonPrimitive(value)
      is Number -> JsonPrimitive(value)
      is String -> JsonPrimitive(value)
      is Map<*, *> ->
        JsonObject(value.entries.associate { (key, item) -> key.toString() to convert(item) })
      is List<*> -> JsonArray(value.map(::convert))
      else -> throw YamlFormatException("Unsupported YAML value of type ${value::class.simpleName}")
    }
}
