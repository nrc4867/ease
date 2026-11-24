package dev.chieppa.ease.agent.query

import dev.chieppa.ease.agent.AgentWorld
import dev.chieppa.ease.agent.tools.AgentTool
import dev.chieppa.ease.agent.tools.ToolAnswer
import dev.chieppa.ease.agent.tools.ToolArgs
import dev.chieppa.ease.agent.tools.ToolInput
import dev.chieppa.ease.agent.tools.ToolNames
import dev.chieppa.ease.commands.EntityFilter
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** The live world's entities with their components as JSON, and its current tick. */
public class QueryTool(private val world: AgentWorld) : AgentTool {
  override val name: String = ToolNames.QUERY
  override val description: String =
    "Reads the live world: its current tick, and its entities in id order, each with every " +
      "component it carries as JSON (component types under the \"type\" key, as describe " +
      "names them). Narrow it with entity ids, or with a component type an entity must carry."
  override val input: ToolInput = ToolInput.of {
    integers(ENTITIES, "Only these entity ids; every entity when absent.")
    string(COMPONENT, "Only entities carrying this component type, such as ease.Quad.")
  }
  override val readOnly: Boolean = true

  override suspend fun call(args: JsonObject): ToolAnswer {
    val filter =
      EntityFilter(ToolArgs.ints(args, ENTITIES)?.toSet(), ToolArgs.string(args, COMPONENT))
    val answer = world.access { hosted ->
      buildJsonObject {
        put(TICK, hosted.tick)
        put(ENTITIES, Json.parseToJsonElement(hosted.codec.worldToJson(hosted.world, filter)))
      }
    }
    return ToolAnswer.text(answer.toString())
  }

  private companion object {
    const val ENTITIES = "entities"
    const val COMPONENT = "component"
    const val TICK = "tick"
  }
}
