package dev.chieppa.ease.ecs

import com.github.quillraven.fleks.IntervalSystem
import com.github.quillraven.fleks.SystemConfiguration
import com.github.quillraven.fleks.World
import com.github.quillraven.fleks.configureWorld
import dev.chieppa.ease.ecs.replication.REPLICATION_INJECTABLE
import dev.chieppa.ease.ecs.replication.Replication
import dev.chieppa.ease.ecs.replication.ReplicationGuardSystem
import dev.chieppa.ease.ecs.replication.ReplicationSettings
import dev.chieppa.ease.ecs.replication.identify
import dev.chieppa.ease.ecs.replication.replication
import dev.chieppa.ease.ecs.replication.usePredicted

/** The entity capacity a world starts with. */
public const val DEFAULT_ENTITY_CAPACITY: Int = 512

/**
 * Builds a world whose systems run in [Phase] order on every `update`, whatever order [systems]
 * registers them in. Systems are constructed inside [systems], where the world being built is
 * current. Every entity gets a network id and authority held by the world's peer: `spawnEntity`
 * adds them inside the entity's creation, and an entity made with Fleks's own `entity` gets them
 * right after. Each run of predicted systems is fenced so that only they may write what the world
 * predicts, and `Replication.stepPredicted` runs them alone. With
 * [ReplicationSettings.debugChecks], each update ends by checking that replicated components
 * changed only through [Replication.write]. [injectables] are injected by name beside the world's
 * replication state.
 */
public fun phasedWorld(
  entityCapacity: Int = DEFAULT_ENTITY_CAPACITY,
  replication: ReplicationSettings = ReplicationSettings(),
  injectables: Map<String, Any> = emptyMap(),
  systems: PhasedSystems.() -> Unit,
): World {
  val state = Replication(replication)
  val world =
    configureWorld(entityCapacity) {
      injectables {
        add(REPLICATION_INJECTABLE, state)
        injectables.forEach { (name, value) -> add(name, value) }
      }
      onAddEntity { entity -> this.replication.identify(this, entity) }
      systems { addInOrder(state, PhasedSystems().apply(systems).inRunOrder()) }
    }
  state.attach(world)
  return world
}

/**
 * Adds [ordered] to the world, a gate opening and closing each phase's run of predicted systems,
 * then the debug write check, and hands the predicted runs and the check to [state] for replays.
 * Within a phase, predicted systems must follow one another.
 */
private fun SystemConfiguration.addInOrder(state: Replication, ordered: List<PhasedSystem>) {
  val predicted = ArrayList<IntervalSystem>()
  fun addReplayed(system: IntervalSystem) {
    add(system)
    predicted += system
  }
  var open: Phase? = null
  val closed = HashSet<Phase>()
  for (entry in ordered) {
    if (open != null && (!entry.predicted || entry.phase != open)) {
      addReplayed(PredictionGate.of(open, false, state))
      closed += open
      open = null
    }
    if (entry.predicted && open == null) {
      check(entry.phase !in closed) { "${entry.phase}'s predicted systems must follow one another" }
      open = entry.phase
      addReplayed(PredictionGate.of(entry.phase, true, state))
    }
    if (entry.predicted) addReplayed(entry.system) else add(entry.system)
  }
  open?.let { addReplayed(PredictionGate.of(it, false, state)) }
  if (state.debugChecks) addReplayed(ReplicationGuardSystem(state))
  state.usePredicted(predicted)
}
