package dev.chieppa.ease.agent.describe

import java.net.URL
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

/**
 * The JSON Schemas of every serializable type on a class path, by serial name, as `ease-codegen`
 * wrote them at build time: each module that declares serializable types carries a [RESOURCE]
 * holding an object of its types' schemas. A schema is complete in itself, with the types it refers
 * to under `$defs`.
 */
public class SchemaCatalog private constructor(private val schemas: Map<String, JsonObject>) {
  /** The schema of the type with serial name [serialName], or null when none was generated. */
  public fun schemaOf(serialName: String): JsonObject? = schemas[serialName]

  public companion object {
    /** Where each module's schemas are on the class path. */
    public const val RESOURCE: String = "META-INF/ease/schemas.json"

    /** The schemas of every module visible to [loader]; the first module to name a type wins. */
    public fun load(loader: ClassLoader): SchemaCatalog {
      val schemas = linkedMapOf<String, JsonObject>()
      loader.getResources(RESOURCE).asSequence().forEach { url ->
        parse(url).forEach { (name, schema) -> schemas.putIfAbsent(name, schema) }
      }
      return SchemaCatalog(schemas)
    }

    private fun parse(url: URL): Map<String, JsonObject> =
      Json.parseToJsonElement(url.readText()).jsonObject.mapValues { it.value.jsonObject }
  }
}
