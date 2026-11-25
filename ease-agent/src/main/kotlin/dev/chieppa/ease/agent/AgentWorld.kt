package dev.chieppa.ease.agent

import dev.chieppa.ease.assets.loading.AssetStore
import dev.chieppa.ease.runtime.GameModule
import dev.chieppa.ease.runtime.HostedWorld
import dev.chieppa.ease.runtime.loop.FixedStep
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * The live world agents look at: built from [module], over its content [assets] when it has any, on
 * its first [access], then frozen, since nothing here steps it. Every access runs on [dispatcher],
 * which must run one task at a time on one thread, so the world and the GPU are only ever touched
 * from that thread; tools are called on the server's threads and hop onto it.
 */
public class AgentWorld(
  public val module: GameModule,
  public val fixedStep: FixedStep,
  private val dispatcher: CoroutineDispatcher,
  private val log: HostLog,
  private val assets: AssetStore? = null,
) {
  private var live: HostedWorld? = null

  /** Runs [block] on the world's thread with the live world and gives its result. */
  public suspend fun <T> access(block: (HostedWorld) -> T): T =
    withContext(dispatcher) { block(live ?: load()) }

  /**
   * An empty world from the same module, fixed step and content that a log is replayed into: it
   * neither starts itself nor syncs with the content, so it never takes the live world's file
   * changes.
   */
  public fun replica(): HostedWorld =
    HostedWorld(module, fixedStep, assets = assets, replaying = true)

  private fun load(): HostedWorld =
    HostedWorld(module, fixedStep, assets = assets).also {
      live = it
      log.record(
        "Game loaded: ${module.javaClass.name}, ${it.world.numEntities} entities at tick ${it.tick}"
      )
    }
}
