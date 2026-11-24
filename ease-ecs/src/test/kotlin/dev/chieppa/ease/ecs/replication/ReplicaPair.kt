package dev.chieppa.ease.ecs.replication

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World

/**
 * An authority world and a replica world holding the same entities under the same network ids, the
 * authority's peer holding every one, as two peers of a session would.
 */
internal class ReplicaPair(entities: Int, replicaDebug: Boolean = true) {
  val authority: World = ReplicationWorlds.world(PeerId.HOST)
  val replica: World = ReplicationWorlds.world(PeerId(1), replicaDebug)
  val held: List<Entity> = List(entities) { ReplicationWorlds.lanternEntity(authority) }
  val mirrored: List<Entity> = held.map { entity ->
    ReplicationWorlds.lanternEntity(
      replica,
      holder = PeerId.HOST,
      id = authority.networkIdOf(entity),
    )
  }
  val writer = BitWriter(StatePackets.MAX_BYTES)
  val packets = StatePacketWriter(authority.replication)
  val reader = StatePacketReader(replica.replication)

  /** A packet with every field of every lantern, in [writer]; returns its length. */
  fun writeAll(): Int {
    packets.begin(writer)
    for (index in held.indices) {
      packets.entity(held[index])
      packets.component(Lantern, ALL_FIELDS)
    }
    return packets.end()
  }

  /** The replica's lanterns as text, to compare whole states. */
  fun replicaState(): String = mirrored.joinToString { entity ->
    val lantern = ReplicationWorlds.lanternOf(replica, entity)
    "${lantern.brightness}/${lantern.fuel}/${lantern.charges}/${lantern.lit}"
  }

  companion object {
    const val ALL_FIELDS = 0b1111
  }
}
