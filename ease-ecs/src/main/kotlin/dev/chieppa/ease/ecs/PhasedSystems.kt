package dev.chieppa.ease.ecs

import com.github.quillraven.fleks.IntervalSystem

/**
 * Collects systems with their [Phase]; the world runs them phase by phase. Systems registered
 * inside [predicted] are the world's predicted systems: see `Replication.mayWrite`.
 */
public class PhasedSystems internal constructor() {
  private val registered = mutableListOf<PhasedSystem>()
  private var predicting = false

  /** Registers [system] to run in [phase]. Within a phase, systems run in registration order. */
  public fun add(phase: Phase, system: IntervalSystem) {
    registered += PhasedSystem(phase, system, predicting)
  }

  /**
   * Registers what [block] adds as predicted systems: they run in every world, and on a client they
   * may write the entities it predicts, such as its own character, ahead of the host.
   */
  public fun predicted(block: PhasedSystems.() -> Unit) {
    check(!predicting) { "Predicted systems do not nest" }
    predicting = true
    try {
      block()
    } finally {
      predicting = false
    }
  }

  /** The systems in run order: by phase, then by registration. */
  internal fun inRunOrder(): List<PhasedSystem> = registered.sortedBy { it.phase.ordinal }
}
