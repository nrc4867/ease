package dev.chieppa.ease.commands

import com.github.quillraven.fleks.Snapshot
import dev.chieppa.ease.ecs.replication.NetworkId
import dev.chieppa.ease.ecs.replication.NetworkIdentity
import kotlinx.serialization.Serializable

/** One entity under its network id with everything else it carries, as worlds are written. */
@Serializable
internal class EntityState(val entity: NetworkId, val snapshot: Snapshot) {
  companion object {
    /** The state of the entity [snapshot] holds, its identity lifted out as [entity]. */
    fun of(snapshot: Snapshot): EntityState {
      val identity = snapshot.components.filterIsInstance<NetworkIdentity>().single()
      val rest = snapshot.components.filterNot { it is NetworkIdentity }
      return EntityState(identity.id, Snapshot(rest, snapshot.tags))
    }
  }
}
