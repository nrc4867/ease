package dev.chieppa.ease.codegen.output

import com.google.devtools.ksp.symbol.KSFile
import dev.chieppa.ease.codegen.model.DescribedType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

/**
 * Writes one JSON file with the schema of every serializable type the module declares, by serial
 * name, for the agent `describe` tool and anything else that reads schemas at runtime.
 */
internal class JsonSchemaOutput(private val writer: ResourceWriter) : ModelOutput<DescribedType> {
  private val schemas = linkedMapOf<String, JsonObject>()
  private val sources = mutableSetOf<KSFile>()

  override fun accept(model: DescribedType, source: KSFile) {
    schemas[model.type.serialName] = JsonSchemaSource.of(model)
    sources += source
  }

  override fun finish() {
    if (schemas.isEmpty()) return
    val sorted = JsonObject(schemas.toSortedMap())
    val content = PRETTY.encodeToString(JsonObject.serializer(), sorted) + "\n"
    writer.write(sources, ResourceFiles.SCHEMAS, ResourceFiles.JSON, content)
  }

  private companion object {
    val PRETTY = Json { prettyPrint = true }
  }
}
