package dev.chieppa.ease.agent.tools

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull

/** Reads a tool call's arguments, refusing one of the wrong kind. */
public object ToolArgs {
  private const val NOT_TEXT = "Expected text for"
  private const val NOT_WHOLE = "Expected a whole number for"
  private const val NOT_NUMBER = "Expected a number for"
  private const val NOT_WHOLES = "Expected a list of whole numbers for"

  /** The text argument [name], or null when absent. */
  public fun string(args: JsonObject, name: String): String? {
    val value = args[name] ?: return null
    return (value as? JsonPrimitive)?.takeIf { it.isString }?.content
      ?: throw ToolRefusal("$NOT_TEXT $name")
  }

  /** The whole-number argument [name], or null when absent. */
  public fun int(args: JsonObject, name: String): Int? {
    val value = args[name] ?: return null
    return (value as? JsonPrimitive)?.takeIf { !it.isString }?.intOrNull
      ?: throw ToolRefusal("$NOT_WHOLE $name")
  }

  /** The whole-number argument [name] as a long, or null when absent. */
  public fun long(args: JsonObject, name: String): Long? {
    val value = args[name] ?: return null
    return (value as? JsonPrimitive)?.takeIf { !it.isString }?.longOrNull
      ?: throw ToolRefusal("$NOT_WHOLE $name")
  }

  /** The number argument [name], or null when absent. */
  public fun float(args: JsonObject, name: String): Float? {
    val value = args[name] ?: return null
    return (value as? JsonPrimitive)?.takeIf { !it.isString }?.floatOrNull
      ?: throw ToolRefusal("$NOT_NUMBER $name")
  }

  /** The list-of-whole-numbers argument [name], or null when absent. */
  public fun ints(args: JsonObject, name: String): List<Int>? {
    val value = args[name] ?: return null
    val list = value as? JsonArray ?: throw ToolRefusal("$NOT_WHOLES $name")
    return list.map { element ->
      (element as? JsonPrimitive)?.takeIf { !it.isString }?.intOrNull
        ?: throw ToolRefusal("$NOT_WHOLES $name")
    }
  }
}
