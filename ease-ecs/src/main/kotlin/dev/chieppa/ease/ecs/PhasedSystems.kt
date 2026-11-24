package dev.chieppa.ease.ecs

import com.github.quillraven.fleks.IntervalSystem

/** Collects systems with their [Phase]; the world runs them phase by phase. */
public class PhasedSystems internal constructor() {
  private val registered = mutableListOf<Pair<Phase, IntervalSystem>>()

  /** Registers [system] to run in [phase]. Within a phase, systems run in registration order. */
  public fun add(phase: Phase, system: IntervalSystem) {
    registered += phase to system
  }

  /** The systems in run order: by phase, then by registration. */
  internal fun inRunOrder(): List<IntervalSystem> =
    registered.sortedBy { (phase, _) -> phase.ordinal }.map { (_, system) -> system }
}
