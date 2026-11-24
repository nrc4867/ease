package dev.chieppa.ease.agent.describe

import dev.chieppa.ease.agent.AgentWorld
import dev.chieppa.ease.agent.tools.AgentTool
import dev.chieppa.ease.agent.tools.ToolAnswer
import dev.chieppa.ease.agent.tools.ToolInput
import dev.chieppa.ease.agent.tools.ToolNames
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.putJsonArray

/**
 * The component and command types the live world's codec knows, each as the JSON Schema
 * `ease-codegen` generated for it, read from [catalog]; a registered type with no generated schema
 * is named under `undescribed` instead.
 */
public class DescribeTool(private val world: AgentWorld, private val catalog: SchemaCatalog) :
  AgentTool {
  override val name: String = ToolNames.DESCRIBE
  override val description: String =
    "Lists the component types and command types this world knows, the engine's and the game's, " +
      "each as a JSON Schema (draft 2020-12): descriptions and ranges from the code, nested " +
      "types under \$defs. Query results and commands use each type's title as their \"type\". " +
      "Registered types no schema was generated for are listed under \"undescribed\"."
  override val input: ToolInput = ToolInput.of {}
  override val readOnly: Boolean = true

  override suspend fun call(args: JsonObject): ToolAnswer {
    val codec = world.access { it.codec }
    val components = codec.componentDescriptors.map(SerialDescriptor::serialName)
    val commands = codec.commandDescriptors.map(SerialDescriptor::serialName)
    val described = buildJsonObject {
      putJsonArray(COMPONENTS) {
        components.mapNotNull { catalog.schemaOf(it) }.forEach { add(it) }
      }
      putJsonArray(COMMANDS) { commands.mapNotNull { catalog.schemaOf(it) }.forEach { add(it) } }
      putJsonArray(UNDESCRIBED) {
        (components + commands)
          .filter { catalog.schemaOf(it) == null }
          .forEach { add(JsonPrimitive(it)) }
      }
    }
    return ToolAnswer.text(described.toString())
  }

  private companion object {
    const val COMPONENTS = "components"
    const val COMMANDS = "commands"
    const val UNDESCRIBED = "undescribed"
  }
}
