package dev.chieppa.ease.agent.describe

import dev.chieppa.ease.agent.AgentWorld
import dev.chieppa.ease.agent.tools.AgentTool
import dev.chieppa.ease.agent.tools.ToolAnswer
import dev.chieppa.ease.agent.tools.ToolInput
import dev.chieppa.ease.agent.tools.ToolNames
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.putJsonArray

/**
 * The component and command types the live world's codec knows, each as the JSON Schema
 * `ease-codegen` generated for it, read from [catalog].
 */
public class DescribeTool(private val world: AgentWorld, private val catalog: SchemaCatalog) :
  AgentTool {
  override val name: String = ToolNames.DESCRIBE
  override val description: String =
    "Lists the component types and command types this world knows, the engine's and the game's, " +
      "each as a JSON Schema (draft 2020-12): descriptions and ranges from the code, nested " +
      "types under \$defs. Query results and commands use each type's title as their \"type\"."
  override val input: ToolInput = ToolInput.of {}
  override val readOnly: Boolean = true

  override suspend fun call(args: JsonObject): ToolAnswer {
    val codec = world.access { it.codec }
    val components = codec.componentDescriptors.map(SerialDescriptor::serialName)
    val commands = codec.commandDescriptors.map(SerialDescriptor::serialName)
    val missing = (components + commands).filter { catalog.schemaOf(it) == null }
    if (missing.isNotEmpty()) {
      return ToolAnswer.error("No generated schema for ${missing.joinToString()}")
    }
    val described = buildJsonObject {
      putJsonArray(COMPONENTS) { components.forEach { add(schema(it)) } }
      putJsonArray(COMMANDS) { commands.forEach { add(schema(it)) } }
    }
    return ToolAnswer.text(described.toString())
  }

  private fun schema(serialName: String): JsonObject = checkNotNull(catalog.schemaOf(serialName))

  private companion object {
    const val COMPONENTS = "components"
    const val COMMANDS = "commands"
  }
}
