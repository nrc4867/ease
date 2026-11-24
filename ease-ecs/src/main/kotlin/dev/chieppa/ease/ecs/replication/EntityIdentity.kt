package dev.chieppa.ease.ecs.replication

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.EntityCreateContext
import com.github.quillraven.fleks.World

/**
 * Creates an entity carrying what [configure] adds and, unless that includes them, a new network id
 * and authority held by this world. Both are added inside the entity's creation, so family hooks
 * and `FamilyOnAdd` systems already see them. Engine code creates entities through this.
 */
public fun World.spawnEntity(configure: EntityCreateContext.(Entity) -> Unit = {}): Entity {
  val state = replication
  state.attach(this)
  return entity { created ->
    configure(created)
    if (!created.has(NetworkIdentity)) created += NetworkIdentity(state.entities.issue())
    if (!created.has(Authority)) created += Authority(state.localPeer, state.localPeer)
  }
}

/**
 * Gives a new [entity] of [world] a network id and authority held by this world, unless it carries
 * them: the fallback for entities created with Fleks's own `entity`, whose families are told about
 * them before this runs.
 */
internal fun Replication.identify(world: World, entity: Entity) {
  attach(world)
  with(world) {
    if (entity.has(NetworkIdentity) && entity.has(Authority)) return
    entity.configure {
      if (!it.has(NetworkIdentity)) it += NetworkIdentity(entities.issue())
      if (!it.has(Authority)) it += Authority(localPeer, localPeer)
    }
  }
}

/** [entity] as messages name it: its network id and its Fleks id. */
internal fun Replication.describe(entity: Entity): String {
  val id = with(world) { entity.getOrNull(NetworkIdentity)?.id?.toString() ?: "without an id" }
  return "Entity $id (Fleks ${entity.id})"
}

/** The peer holding authority over [entity], as messages name it. */
internal fun Replication.holderOf(entity: Entity): String =
  with(world) { entity.getOrNull(Authority)?.holder?.toString() ?: "no peer" }
