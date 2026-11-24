package dev.chieppa.ease.agent.tools

import dev.chieppa.ease.agent.AgentWorld
import dev.chieppa.ease.agent.HostLog
import dev.chieppa.ease.agent.capture.CaptureTool
import dev.chieppa.ease.agent.capture.Capturer
import dev.chieppa.ease.agent.checks.TestTool
import dev.chieppa.ease.agent.checks.WorldChecks
import dev.chieppa.ease.agent.describe.DescribeTool
import dev.chieppa.ease.agent.logs.LogsTool
import dev.chieppa.ease.agent.query.QueryTool

/** The tools the agent server offers on a world. */
public object AgentTools {
  /** Every tool, on [world], capturing with [capturer] and reading and writing [log]. */
  public fun all(world: AgentWorld, capturer: Capturer, log: HostLog): List<AgentTool> =
    listOf(
      DescribeTool(world),
      QueryTool(world),
      CaptureTool(world, capturer),
      LogsTool(log),
      TestTool(WorldChecks(world, log)),
    )
}
