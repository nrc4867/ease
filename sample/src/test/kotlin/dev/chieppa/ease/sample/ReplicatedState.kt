package dev.chieppa.ease.sample

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import com.github.quillraven.fleks.World.Companion.family
import dev.chieppa.ease.ecs.replication.Authority
import dev.chieppa.ease.ecs.replication.NetworkIdentity
import dev.chieppa.ease.ecs.replication.ReplicatedSerializer
import dev.chieppa.ease.ecs.replication.networkIdOf
import dev.chieppa.ease.scene.node.NodeKey

/**
 * One replicated entity as a world held it at one moment: its network id, who held and owned it,
 * its node key when it is a scene node, and a copy of each replicated component by serial name.
 */
internal class EntityState(
  val id: Long,
  val holder: Long,
  val owner: Long,
  val key: String?,
  val components: Map<String, Component<*>>,
)

/** Copies of the replicated state of every entity a world holds, for comparing later. */
internal class ReplicatedState(private val serializers: List<ReplicatedSerializer<*>>) {
  /** Every entity with a network id in [world], by network id, its components copied. */
  fun of(world: World): Map<Long, EntityState> {
    val states = HashMap<Long, EntityState>()
    with(world) {
      family { all(NetworkIdentity) }
        .forEach { entity ->
          val id = world.networkIdOf(entity).value
          val authority = entity.getOrNull(Authority)
          states[id] =
            EntityState(
              id,
              authority?.holder?.value?.toLong() ?: -1,
              authority?.owner?.value?.toLong() ?: -1,
              entity.getOrNull(NodeKey)?.text,
              components(world, entity),
            )
        }
    }
    return states
  }

  private fun components(world: World, entity: Entity): Map<String, Component<*>> =
    with(world) {
      serializers.filter { entity.has(it.type) }.associate { it.name to copy(it, world, entity) }
    }

  private fun <T : Component<T>> copy(
    serializer: ReplicatedSerializer<T>,
    world: World,
    entity: Entity,
  ): T = serializer.create().also { serializer.copy(serializer.componentOf(world, entity), it) }
}
