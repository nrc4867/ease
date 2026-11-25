package dev.chieppa.ease.ecs.replication

import com.github.quillraven.fleks.Entity

/**
 * Which live entity carries each [NetworkId] in one world, and the next id to issue. Ids are issued
 * once per session and never again, even after their entity is gone. The host's world, the
 * session's authority, issues from 1 up to [NetworkId.LOCAL_START], the ids that go on the wire;
 * every other world issues its own entities' ids from there up to [NetworkId.LIMIT], so an id the
 * host sends never meets one a client issued.
 */
public class NetworkEntities internal constructor(host: Boolean) {
  private val entityIds = LongIntMap()
  private val first = if (host) 1L else NetworkId.LOCAL_START
  private val end = if (host) NetworkId.LOCAL_START else NetworkId.LIMIT
  private var next = first

  /** The live entity carrying [id], or [Entity.NONE] when none does. */
  public fun entityOf(id: NetworkId): Entity {
    val found = entityIds.get(id.value, NO_ENTITY)
    return if (found == NO_ENTITY) Entity.NONE else Entity(found)
  }

  /** Whether a live entity carries [id]. */
  public operator fun contains(id: NetworkId): Boolean =
    entityIds.get(id.value, NO_ENTITY) != NO_ENTITY

  /**
   * A new id no entity has carried this session. Throws once every id of this world's range has
   * been issued, which ends the session rather than reusing one.
   */
  internal fun issue(): NetworkId {
    check(next < end) { "Every network id of this session has been issued" }
    return NetworkId(next++)
  }

  /**
   * Records that [entity] carries [id]. An id in this world's own range, such as one a restore
   * brings back, moves the next issued id past it; one from the other range, such as the host's on
   * a client, leaves it alone.
   */
  internal fun register(id: NetworkId, entity: Entity) {
    require(id.isValid) { "Network ids run from 1 to below ${NetworkId.LIMIT}, not ${id.value}" }
    check(id !in this) { "Network id $id is already carried by entity ${entityOf(id).id}" }
    entityIds.put(id.value, entity.id)
    if (id.value in next until end) next = id.value + 1
  }

  /** Records that no live entity carries [id] any more; the id is never issued again. */
  internal fun release(id: NetworkId) {
    entityIds.remove(id.value)
  }

  private companion object {
    const val NO_ENTITY = -1
  }
}
