package dev.chieppa.ease.agent

import dev.chieppa.ease.agent.capture.Capturer
import dev.chieppa.ease.agent.server.AgentServer
import dev.chieppa.ease.agent.server.ConnectCommand
import dev.chieppa.ease.agent.tools.AgentTools
import dev.chieppa.ease.assets.loading.AssetStore
import dev.chieppa.ease.runtime.GameModule
import dev.chieppa.ease.runtime.loop.FixedStep
import kotlinx.coroutines.CoroutineDispatcher

/**
 * The headless agent host: [module]'s world, over its content [assets] when it has any, frozen,
 * served to agents over MCP with [token]. All world and GPU work runs on [dispatcher], which must
 * run one task at a time on one thread. The GPU is opened by the first capture.
 */
public class AgentHost(
  module: GameModule,
  token: String,
  dispatcher: CoroutineDispatcher,
  public val log: HostLog = HostLog(),
  fixedStep: FixedStep = FixedStep.perSecond(),
  assets: AssetStore? = null,
) {
  /** The live world the tools read. */
  public val world: AgentWorld = AgentWorld(module, fixedStep, dispatcher, log, assets)

  private val capturer = Capturer()
  private val server = AgentServer(AgentTools.all(world, capturer, log), token, log)

  /** Loads the game, then listens on [port] (0 for any free one) and gives the port. */
  public suspend fun start(port: Int): Int {
    world.access {}
    val bound = server.start(port)
    log.record("Agent server listening on ${ConnectCommand.url(bound)}")
    return bound
  }

  /** Stops listening and releases the GPU. */
  public suspend fun stop() {
    server.stop()
    world.access { capturer.close() }
  }
}
