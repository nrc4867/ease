package dev.chieppa.ease.agent

import dev.chieppa.ease.runtime.GameModule
import dev.chieppa.ease.runtime.HostedWorld
import dev.chieppa.ease.runtime.loop.FixedStep
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * The live world agents look at: built from [module] on its first [access], then frozen, since
 * nothing here steps it. Every access runs on [dispatcher], which must run one task at a time on
 * one thread, so the world and the GPU are only ever touched from that thread; tools are called on
 * the server's threads and hop onto it.
 */
public class AgentWorld(
  public val module: GameModule,
  public val fixedStep: FixedStep,
  private val dispatcher: CoroutineDispatcher,
  private val log: HostLog,
) {
  private var live: HostedWorld? = null

  /** Runs [block] on the world's thread with the live world and gives its result. */
  public suspend fun <T> access(block: (HostedWorld) -> T): T =
    withContext(dispatcher) { block(live ?: load()) }

  /** A fresh world from the same module and fixed step, populated but never stepped. */
  public fun fresh(): HostedWorld = HostedWorld(module, fixedStep)

  private fun load(): HostedWorld =
    fresh().also {
      live = it
      log.record(
        "Game loaded: ${module.javaClass.name}, ${it.world.numEntities} entities at tick ${it.tick}"
      )
    }
}
