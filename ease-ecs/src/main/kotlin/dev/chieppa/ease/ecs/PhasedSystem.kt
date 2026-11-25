package dev.chieppa.ease.ecs

import com.github.quillraven.fleks.IntervalSystem

/** A [system] registered to run in [phase], and whether it is a predicted system. */
internal class PhasedSystem(
  val phase: Phase,
  val system: IntervalSystem,
  val predicted: Boolean,
)
