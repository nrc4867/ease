package dev.chieppa.ease.serialization.yaml

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import org.snakeyaml.engine.v2.exceptions.YamlEngineException

/**
 * The text format of meta files and scenes: YAML written one way only, so the same content always
 * gives the same bytes and git merges it line by line.
 *
 * Keys are sorted, nesting is two spaces, a string is bare only when every reader reads it back as
 * a string and double-quoted otherwise, numbers keep the shortest form that reads back exactly, and
 * nothing is a timestamp. Reading accepts any YAML snakeyaml-engine does.
 */
public object CanonicalYaml {
  private val json = Json { encodeDefaults = true }

  /** [element] as canonical YAML. */
  public fun write(element: JsonElement): String = YamlWriter().write(element)

  /** The tree [text] holds. Throws [YamlFormatException] when it is not valid YAML. */
  public fun read(text: String): JsonElement =
    try {
      YamlReader.read(text)
    } catch (failure: YamlEngineException) {
      throw YamlFormatException("Not valid YAML: ${failure.message}", failure)
    }

  /** [value] as canonical YAML, through its [serializer]. */
  public fun <T> encode(serializer: KSerializer<T>, value: T): String =
    write(json.encodeToJsonElement(serializer, value))

  /** [value] as canonical YAML like [encode], leaving out every top-level field that is null. */
  public fun <T> encodeWithoutNulls(serializer: KSerializer<T>, value: T): String {
    val tree = json.encodeToJsonElement(serializer, value) as JsonObject
    return write(JsonObject(tree.filterValues { it !is JsonNull }))
  }

  /** The value [text] holds, read through [serializer]. */
  public fun <T> decode(serializer: KSerializer<T>, text: String): T =
    json.decodeFromJsonElement(serializer, read(text))
}
