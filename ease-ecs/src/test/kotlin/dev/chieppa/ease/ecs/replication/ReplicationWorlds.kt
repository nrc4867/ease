package dev.chieppa.ease.ecs.replication

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.ecs.LocalTransformReplicated
import dev.chieppa.ease.ecs.phasedWorld

/** Worlds and entities for the replication tests. */
internal object ReplicationWorlds {
  val serializers: List<ReplicatedSerializer<*>> =
    listOf(LanternReplicated, LocalTransformReplicated)

  /** A world running as [peer], with [Lantern] and [LocalTransform] replicated. */
  fun world(peer: PeerId = PeerId.HOST, debug: Boolean = true): World =
    phasedWorld(replication = ReplicationSettings(peer, serializers, debug)) {}

  /** An entity carrying a lantern and a transform, in [world], held by [holder]. */
  fun lanternEntity(world: World, holder: PeerId? = null, id: NetworkId? = null): Entity =
    world.entity {
      it += Lantern()
      it += LocalTransform()
      if (holder != null) it += Authority(holder, holder)
      if (id != null) it += NetworkIdentity(id)
    }

  fun lanternOf(world: World, entity: Entity): Lantern = with(world) { entity[Lantern] }
}
