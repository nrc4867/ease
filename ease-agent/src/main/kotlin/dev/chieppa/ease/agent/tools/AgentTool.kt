package dev.chieppa.ease.agent.tools

import kotlinx.serialization.json.JsonObject

/**
 * One tool on the agent server: its [name], the [description] a client's model reads, the JSON
 * schema of its [input], and what calling it with arguments gives. [readOnly] tools change nothing
 * in the live world. A tool refuses what it cannot do by throwing [ToolRefusal]. A new tool is a
 * new implementation added to [AgentTools].
 */
public interface AgentTool {
  public val name: String
  public val description: String
  public val input: ToolInput
  public val readOnly: Boolean

  public suspend fun call(args: JsonObject): ToolAnswer
}
