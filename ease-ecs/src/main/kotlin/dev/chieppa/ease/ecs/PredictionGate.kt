package dev.chieppa.ease.ecs

import com.github.quillraven.fleks.IntervalSystem
import dev.chieppa.ease.ecs.replication.Replication

/**
 * Opens or closes one phase's run of predicted systems: it sets [Replication.predicting] to
 * [opens], so only the systems between a phase's opening and closing gate may write the entities
 * the world predicts. A world holds each system class once, so each phase has its own pair.
 */
internal sealed class PredictionGate(
  private val replication: Replication,
  private val opens: Boolean,
) : IntervalSystem() {
  override fun onTick() {
    replication.predicting = opens
  }

  class OpenInput(replication: Replication) : PredictionGate(replication, true)

  class CloseInput(replication: Replication) : PredictionGate(replication, false)

  class OpenSimulation(replication: Replication) : PredictionGate(replication, true)

  class CloseSimulation(replication: Replication) : PredictionGate(replication, false)

  class OpenPhysics(replication: Replication) : PredictionGate(replication, true)

  class ClosePhysics(replication: Replication) : PredictionGate(replication, false)

  class OpenAnimation(replication: Replication) : PredictionGate(replication, true)

  class CloseAnimation(replication: Replication) : PredictionGate(replication, false)

  class OpenExtract(replication: Replication) : PredictionGate(replication, true)

  class CloseExtract(replication: Replication) : PredictionGate(replication, false)

  companion object {
    /** The gate opening (or, unless [opens], closing) [phase]'s predicted systems. */
    fun of(phase: Phase, opens: Boolean, replication: Replication): PredictionGate =
      when (phase) {
        Phase.INPUT -> if (opens) OpenInput(replication) else CloseInput(replication)
        Phase.SIMULATION -> if (opens) OpenSimulation(replication) else CloseSimulation(replication)
        Phase.PHYSICS -> if (opens) OpenPhysics(replication) else ClosePhysics(replication)
        Phase.ANIMATION -> if (opens) OpenAnimation(replication) else CloseAnimation(replication)
        Phase.EXTRACT -> if (opens) OpenExtract(replication) else CloseExtract(replication)
      }
  }
}
