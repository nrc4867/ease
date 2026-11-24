package dev.chieppa.ease.agent

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Field [name] of this JSON object. */
public fun JsonElement.field(name: String): JsonElement = jsonObject.getValue(name)

/** Field [name] of this JSON object, as text. */
public fun JsonElement.text(name: String): String = field(name).jsonPrimitive.content

/** Field [name] of this JSON object, as an array. */
public fun JsonElement.list(name: String): JsonArray = field(name).jsonArray

/** Field [name] of this JSON object, as an object. */
public fun JsonElement.obj(name: String): JsonObject = field(name).jsonObject

/** The `name` of each object in this array. */
public fun JsonArray.names(): List<String> = map { it.text("name") }
