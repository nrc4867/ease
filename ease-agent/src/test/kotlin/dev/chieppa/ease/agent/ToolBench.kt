package dev.chieppa.ease.agent

import dev.chieppa.ease.agent.capture.Capturer
import dev.chieppa.ease.agent.tools.AgentTools
import dev.chieppa.ease.agent.tools.ToolAnswer
import dev.chieppa.ease.agent.tools.ToolPart
import dev.chieppa.ease.gpu.GpuContext
import dev.chieppa.ease.runtime.GameModule
import dev.chieppa.ease.runtime.loop.FixedStep
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * The agent tools on a [module]'s world, called directly, with world access on [dispatcher] and the
 * GPU opened by [openGpu].
 */
internal class ToolBench(
  dispatcher: CoroutineDispatcher,
  module: GameModule = DriftGame(),
  openGpu: () -> GpuContext = { GpuContext.open() },
) {
  val log = HostLog()
  val world = AgentWorld(module, FixedStep.perSecond(), dispatcher, log)
  val capturer = Capturer(openGpu)
  private val tools = AgentTools.all(world, capturer, log).associateBy { it.name }

  suspend fun call(name: String, args: JsonObject = JsonObject(emptyMap())): ToolAnswer =
    tools.getValue(name).call(args)

  /** The JSON of [answer]'s first text part. */
  fun json(answer: ToolAnswer): JsonElement {
    val first = answer.parts.filterIsInstance<ToolPart.Text>().first()
    return Json.parseToJsonElement(first.text)
  }

  /** Runs [ticks] ticks on the live world, as a host playing it would. */
  suspend fun stepLive(ticks: Int) = world.access { hosted -> repeat(ticks) { hosted.step() } }
}
