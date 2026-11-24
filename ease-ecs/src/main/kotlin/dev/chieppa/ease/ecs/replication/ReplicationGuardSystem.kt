package dev.chieppa.ease.ecs.replication

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.Family
import com.github.quillraven.fleks.IntervalSystem
import com.github.quillraven.fleks.World.Companion.family

/**
 * The debug build's end-of-tick check: every replicated component must hold what it held after its
 * last [Replication.write] or received change. One changed any other way throws
 * [AuthorityViolation], naming the entity and component, since its change would never replicate.
 */
internal class ReplicationGuardSystem(private val replication: Replication) : IntervalSystem() {
  private val families: Array<Family> =
    Array(replication.types.size) { id ->
      val type = replication.types[id].serializer.type
      family { all(type) }
    }

  override fun onTick() {
    for (id in families.indices) {
      val type = replication.types[id]
      families[id].forEach { entity ->
        if (!type.unchangedSinceKnown(world, entity)) violation(entity, id)
      }
    }
  }

  private fun violation(entity: Entity, id: Int): Nothing {
    val name = replication.types[id].serializer.name
    val how =
      if (replication.hasAuthority(entity)) "outside Replication.write, so it would not replicate"
      else "without authority"
    throw AuthorityViolation("${replication.describe(entity)} changed $name $how")
  }
}
