package dev.chieppa.ease.ecs.replication

import com.github.quillraven.fleks.Entity

/** Gives a new [entity] a network id and authority held by this world, unless it carries them. */
internal fun Replication.identify(entity: Entity) {
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
