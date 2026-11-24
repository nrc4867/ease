package dev.chieppa.ease.agent.logs

import dev.chieppa.ease.agent.HostLog
import dev.chieppa.ease.agent.tools.AgentTool
import dev.chieppa.ease.agent.tools.ToolAnswer
import dev.chieppa.ease.agent.tools.ToolArgs
import dev.chieppa.ease.agent.tools.ToolInput
import dev.chieppa.ease.agent.tools.ToolNames
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

/** The lines the host recorded since a marker, and the marker to ask from next. */
public class LogsTool(private val log: HostLog) : AgentTool {
  override val name: String = ToolNames.LOGS
  override val description: String =
    "Reads what the host recorded since a marker: server start, the game loading, ticks run by " +
      "test, rejected commands and tool errors, oldest first. Pass the returned \"next\" as the " +
      "marker to read only newer lines; \"dropped\" counts lines too old to be kept."
  override val input: ToolInput = ToolInput.of {
    integer(MARKER, "The first sequence number to read; 0 when absent.", 0..Long.MAX_VALUE)
    integer(LIMIT, "How many lines to give at most; $DEFAULT_LIMIT when absent.", 1L..MAX_LIMIT)
  }
  override val readOnly: Boolean = true

  override suspend fun call(args: JsonObject): ToolAnswer {
    val marker = (ToolArgs.long(args, MARKER) ?: 0L).coerceAtLeast(0L)
    val limit = (ToolArgs.int(args, LIMIT) ?: DEFAULT_LIMIT).coerceIn(1, MAX_LIMIT.toInt())
    val page = log.since(marker, limit)
    val answer = buildJsonObject {
      putJsonArray(LINES) {
        page.lines.forEach { line ->
          add(
            buildJsonObject {
              put(SEQUENCE, line.sequence)
              put(TEXT, line.text)
            }
          )
        }
      }
      put(NEXT, page.next)
      put(DROPPED, page.dropped)
    }
    return ToolAnswer.text(answer.toString())
  }

  private companion object {
    const val MARKER = "marker"
    const val LIMIT = "limit"
    const val DEFAULT_LIMIT = 200
    const val MAX_LIMIT = 1_000L
    const val LINES = "lines"
    const val SEQUENCE = "sequence"
    const val TEXT = "text"
    const val NEXT = "next"
    const val DROPPED = "dropped"
  }
}
