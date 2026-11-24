package dev.chieppa.ease.agent.checks

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * Where two JSON documents differ, as readable lines: the path to each differing value, then both
 * values. Stops after [limit] lines.
 */
public class WorldDiff(private val limit: Int = DEFAULT_LIMIT) {
  /** The differences between [expected] and [actual], named [expectedLabel] and [actualLabel]. */
  public fun between(
    expected: String,
    actual: String,
    expectedLabel: String,
    actualLabel: String,
  ): List<String> {
    val found = mutableListOf<String>()
    compare(ROOT, Json.parseToJsonElement(expected), Json.parseToJsonElement(actual)) { path, a, b
      ->
      if (found.size < limit)
        found += "$path: $expectedLabel ${a ?: MISSING}, $actualLabel ${b ?: MISSING}"
    }
    return found
  }

  private fun compare(
    path: String,
    a: JsonElement?,
    b: JsonElement?,
    report: (String, JsonElement?, JsonElement?) -> Unit,
  ) {
    when {
      a is JsonObject && b is JsonObject ->
        (a.keys + b.keys).forEach { key -> compare("$path.$key", a[key], b[key], report) }
      a is JsonArray && b is JsonArray ->
        repeat(maxOf(a.size, b.size)) { index ->
          compare("$path[$index]", a.getOrNull(index), b.getOrNull(index), report)
        }
      a != b -> report(path, a, b)
    }
  }

  private companion object {
    const val DEFAULT_LIMIT = 20
    const val ROOT = "world"
    const val MISSING = "(missing)"
  }
}
