package dev.chieppa.ease.ecs.replication

import com.github.quillraven.fleks.Entity

/**
 * Which live entity carries each [NetworkId] in one world, and the next id to issue. Ids are issued
 * once per session and never again, even after their entity is gone.
 */
public class NetworkEntities internal constructor() {
  private val entityIds = LongIntMap()
  private var next = 1L

  /** The live entity carrying [id], or [Entity.NONE] when none does. */
  public fun entityOf(id: NetworkId): Entity {
    val found = entityIds.get(id.value, NO_ENTITY)
    return if (found == NO_ENTITY) Entity.NONE else Entity(found)
  }

  /** Whether a live entity carries [id]. */
  public operator fun contains(id: NetworkId): Boolean =
    entityIds.get(id.value, NO_ENTITY) != NO_ENTITY

  /** A new id no entity has carried this session. */
  internal fun issue(): NetworkId = NetworkId(next++)

  /**
   * Records that [entity] carries [id]. An id from elsewhere, such as an authority's, moves the
   * next issued id past it.
   */
  internal fun register(id: NetworkId, entity: Entity) {
    require(id.value > 0) { "Network ids start at 1, not ${id.value}" }
    check(id !in this) { "Network id $id is already carried by entity ${entityOf(id).id}" }
    entityIds.put(id.value, entity.id)
    if (id.value >= next) next = id.value + 1
  }

  /** Records that no live entity carries [id] any more; the id is never issued again. */
  internal fun release(id: NetworkId) {
    entityIds.remove(id.value)
  }

  private companion object {
    const val NO_ENTITY = -1
  }
}
