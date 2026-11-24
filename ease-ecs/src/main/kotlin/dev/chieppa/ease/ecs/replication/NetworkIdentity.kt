package dev.chieppa.ease.ecs.replication

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World

/**
 * An entity's [NetworkId]. Every entity of a phased world carries one from the moment it is
 * created; adding it registers the id with the world's [NetworkEntities], removing it releases the
 * entity but never the id.
 */
public class NetworkIdentity(public val id: NetworkId) : Component<NetworkIdentity> {
  override fun type(): ComponentType<NetworkIdentity> = NetworkIdentity

  override fun World.onAdd(entity: Entity) {
    replication.entities.register(id, entity)
  }

  override fun World.onRemove(entity: Entity) {
    replication.entities.release(id)
  }

  /** The component type. */
  public companion object : ComponentType<NetworkIdentity>()
}
