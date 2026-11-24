package dev.chieppa.ease.agent.checks

import dev.chieppa.ease.agent.tools.AgentTool
import dev.chieppa.ease.agent.tools.ToolAnswer
import dev.chieppa.ease.agent.tools.ToolArgs
import dev.chieppa.ease.agent.tools.ToolInput
import dev.chieppa.ease.agent.tools.ToolNames
import dev.chieppa.ease.agent.tools.ToolRefusal
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

/** Runs a headless check on a scratch copy of the live world and reports pass or fail. */
public class TestTool(private val checks: WorldChecks) : AgentTool {
  override val name: String = ToolNames.TEST
  override val description: String =
    "Runs a headless check on a scratch copy of the live world, which it leaves untouched. " +
      "\"replay\": rebuilds the world from the game and its command log at the logged ticks and " +
      "compares it with the live one. \"ticks\": runs the copy that many more ticks and passes " +
      "when nothing throws. Answers passed, the tick reached and the failures."
  override val input: ToolInput = ToolInput.of {
    string(CHECK, "Which check to run.", required = true, oneOf = listOf(REPLAY, TICKS))
    integer(TICK_COUNT, "Ticks for \"ticks\"; $DEFAULT_TICKS when absent.", 1L..MAX_TICKS)
  }
  override val readOnly: Boolean = true

  override suspend fun call(args: JsonObject): ToolAnswer {
    val result =
      when (val check = ToolArgs.string(args, CHECK)) {
        REPLAY -> checks.replay()
        TICKS ->
          checks.ticks(
            ToolArgs.int(args, TICK_COUNT)?.coerceIn(1, MAX_TICKS.toInt()) ?: DEFAULT_TICKS
          )
        else -> throw ToolRefusal("Unknown check $check; use $REPLAY or $TICKS")
      }
    val answer = buildJsonObject {
      put(CHECK, result.check)
      put(PASSED, result.passed)
      put(TICK, result.tick)
      putJsonArray(FAILURES) { result.failures.forEach { add(it) } }
    }
    return ToolAnswer.text(answer.toString())
  }

  private companion object {
    const val CHECK = "check"
    const val REPLAY = WorldChecks.REPLAY
    const val TICKS = WorldChecks.TICKS
    const val TICK_COUNT = "ticks"
    const val DEFAULT_TICKS = 60
    const val MAX_TICKS = 100_000L
    const val PASSED = "passed"
    const val TICK = "tick"
    const val FAILURES = "failures"
  }
}
