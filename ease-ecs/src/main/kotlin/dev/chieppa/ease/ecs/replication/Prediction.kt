package dev.chieppa.ease.ecs.replication

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IntervalSystem

/**
 * Whether this world predicts [entity]: the world's peer owns it but another peer holds it, as a
 * client's own character is held by the host.
 */
public fun Replication.predicts(entity: Entity): Boolean {
  val authority = with(world) { entity.getOrNull(Authority) } ?: return false
  return authority.owner == localPeer && authority.holder != localPeer
}

/**
 * Whether code running now may change [entity]'s replicated state: with authority over it, or
 * inside a predicted system when this world [predicts] it.
 */
public fun Replication.mayWrite(entity: Entity): Boolean =
  hasAuthority(entity) || (predicting && predicts(entity))

/**
 * Runs the predicted systems alone for one more tick as long as the world's last update, as a
 * client replaying its input does; nothing else in the world moves, and [Replication.replaying] is
 * true while they run. In a debug build the write check runs after them.
 */
public fun Replication.stepPredicted() {
  val systems = predictedSystems
  replaying = true
  try {
    for (index in systems.indices) {
      val system = systems[index]
      if (system.enabled) system.onUpdate()
    }
  } finally {
    replaying = false
  }
}

/** [systems] are what [stepPredicted] runs, in order. */
internal fun Replication.usePredicted(systems: List<IntervalSystem>) {
  predictedSystems = systems.toTypedArray()
}
