package dev.chieppa.ease.agent.server

import dev.chieppa.ease.agent.HostLog
import dev.chieppa.ease.agent.tools.AgentTool
import dev.chieppa.ease.agent.tools.ToolAnswer
import dev.chieppa.ease.agent.tools.ToolPart
import dev.chieppa.ease.agent.tools.ToolRefusal
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.ImageContent
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.Tool
import io.modelcontextprotocol.kotlin.sdk.types.ToolAnnotations
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import java.util.Base64
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonObject

/**
 * The tools as the MCP SDK lists and calls them. A refusal or failure answers as an error result
 * the model reads, and is recorded in [log]; it never takes the server down.
 */
internal class ToolCalls(private val log: HostLog) {
  fun describe(tool: AgentTool): Tool =
    Tool(
      name = tool.name,
      inputSchema =
        ToolSchema(
          properties = tool.input.properties,
          required = tool.input.required.ifEmpty { null },
        ),
      description = tool.description,
      annotations =
        ToolAnnotations(
          readOnlyHint = tool.readOnly,
          destructiveHint = false,
          openWorldHint = false,
        ),
    )

  @Suppress("TooGenericExceptionCaught") // Any tool failure becomes an error answer.
  suspend fun call(tool: AgentTool, args: JsonObject?): CallToolResult {
    val answer =
      try {
        tool.call(args ?: JsonObject(emptyMap()))
      } catch (cancelled: CancellationException) {
        throw cancelled
      } catch (refusal: ToolRefusal) {
        failed(tool, refusal.message.orEmpty())
      } catch (failure: Exception) {
        failed(tool, "$FAILED ${failure.message ?: failure}")
      }
    return result(answer)
  }

  private fun failed(tool: AgentTool, reason: String): ToolAnswer {
    log.record("Tool ${tool.name} failed: $reason")
    return ToolAnswer.error(reason)
  }

  private fun result(answer: ToolAnswer): CallToolResult =
    CallToolResult(
      content =
        answer.parts.map { part ->
          when (part) {
            is ToolPart.Text -> TextContent(part.text)
            is ToolPart.Png -> ImageContent(Base64.getEncoder().encodeToString(part.bytes), PNG)
          }
        },
      isError = answer.error.takeIf { it },
    )

  private companion object {
    const val PNG = "image/png"
    const val FAILED = "The tool failed:"
  }
}
