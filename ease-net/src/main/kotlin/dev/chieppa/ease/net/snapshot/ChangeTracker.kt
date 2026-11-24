package dev.chieppa.ease.net.snapshot

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.Family
import com.github.quillraven.fleks.World
import dev.chieppa.ease.ecs.replication.NetworkIdentity
import dev.chieppa.ease.ecs.replication.replication

private const val INITIAL_ENTITIES = 64

/**
 * Finds, once per tick, which fields of each replicated component the host holds changed since the
 * last tick, for every peer's snapshot to fold in. A component is named by its slot: its entity's
 * id times the number of replicated types, plus its type's wire id.
 */
internal class ChangeTracker(private val world: World) {
  private val replication = world.replication

  /** How many replicated types the world has, which slots are counted in. */
  val typeCount: Int = replication.types.size

  /** Every entity with a network id, which is every entity. */
  val entities: Family = world.family { all(NetworkIdentity) }

  private val baselines = Array(typeCount) { TypeBaseline.of(replication.types[it].serializer) }
  private var networkIds = LongArray(INITIAL_ENTITIES)

  /** The slot of [entity]'s component with wire id [typeId]. */
  fun slotOf(entity: Entity, typeId: Int): Int = entity.id * typeCount + typeId

  /** Whether [entity] carries the replicated type with wire id [typeId]. */
  fun carries(entity: Entity, typeId: Int): Boolean = replication.types[typeId].isOn(world, entity)

  /** Tells [sink] what changed in every component of every entity this world holds. */
  fun update(sink: ChangeSink) {
    entities.forEach { entity -> if (replication.hasAuthority(entity)) visit(entity, sink) }
  }

  private fun visit(entity: Entity, sink: ChangeSink) {
    val networkId = replication.networkIdOf(entity).value
    val index = entity.id
    if (index >= networkIds.size) {
      networkIds = networkIds.copyOf(maxOf(index + 1, networkIds.size * 2))
    }
    val fresh = networkIds[index] != networkId
    networkIds[index] = networkId
    for (typeId in 0 until typeCount) {
      if (!carries(entity, typeId)) continue
      val baseline = baselines[typeId]
      val fields = baseline.changed(world, entity, fresh)
      sink.changed(slotOf(entity, typeId), networkId, fields)
    }
  }
}
