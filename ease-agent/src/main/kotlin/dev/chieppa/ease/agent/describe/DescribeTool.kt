package dev.chieppa.ease.agent.describe

import dev.chieppa.ease.agent.AgentWorld
import dev.chieppa.ease.agent.tools.AgentTool
import dev.chieppa.ease.agent.tools.ToolAnswer
import dev.chieppa.ease.agent.tools.ToolInput
import dev.chieppa.ease.agent.tools.ToolNames
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.putJsonArray

/** The component and command types the live world's codec knows, each with its schema. */
public class DescribeTool(private val world: AgentWorld) : AgentTool {
  override val name: String = ToolNames.DESCRIBE
  override val description: String =
    "Lists the component types and command types this world knows, the engine's and the game's, " +
      "each with its schema: serial name, kind, and fields with their types, nested types " +
      "spelled out. Query results and commands use these names under the \"type\" key."
  override val input: ToolInput = ToolInput.of {}
  override val readOnly: Boolean = true

  override suspend fun call(args: JsonObject): ToolAnswer {
    val described = world.access { hosted ->
      buildJsonObject {
        putJsonArray(COMPONENTS) {
          hosted.codec.componentDescriptors.forEach { add(TypeSchema.of(it)) }
        }
        putJsonArray(COMMANDS) {
          hosted.codec.commandDescriptors.forEach { add(TypeSchema.of(it)) }
        }
      }
    }
    return ToolAnswer.text(described.toString())
  }

  private companion object {
    const val COMPONENTS = "components"
    const val COMMANDS = "commands"
  }
}
