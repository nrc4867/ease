package dev.chieppa.ease.serialization.yaml

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

private const val INDENT = "  "

/**
 * Writes a [JsonElement] as block-style YAML: object keys sorted, two-space indent, one line per
 * scalar, numbers as the JSON tree spells them, and a final newline.
 */
internal class YamlWriter {
  private val out = StringBuilder()

  fun write(root: JsonElement): String {
    if (fitsOnLine(root)) out.append(inline(root)).append('\n') else block(root, 0)
    return out.toString()
  }

  private fun block(node: JsonElement, depth: Int) {
    when (node) {
      is JsonObject -> node.entries.sortedBy { it.key }.forEach { entry(it.key, it.value, depth) }
      is JsonArray -> node.forEach { item(it, depth) }
      else -> error("Only objects and arrays have blocks")
    }
  }

  private fun entry(key: String, value: JsonElement, depth: Int) {
    out.append(INDENT.repeat(depth)).append(YamlScalars.string(key)).append(':')
    child(value, depth)
  }

  private fun item(value: JsonElement, depth: Int) {
    out.append(INDENT.repeat(depth)).append('-')
    child(value, depth)
  }

  private fun child(value: JsonElement, depth: Int) {
    if (fitsOnLine(value)) {
      out.append(' ').append(inline(value)).append('\n')
    } else {
      out.append('\n')
      block(value, depth + 1)
    }
  }

  /** Scalars, and the empty object and array, which fit on their key's line. */
  private fun fitsOnLine(node: JsonElement): Boolean =
    when (node) {
      is JsonObject -> node.isEmpty()
      is JsonArray -> node.isEmpty()
      else -> true
    }

  private fun inline(node: JsonElement): String =
    when (node) {
      is JsonObject -> "{}"
      is JsonArray -> "[]"
      JsonNull -> "null"
      is JsonPrimitive -> if (node.isString) YamlScalars.string(node.content) else node.content
    }
}
