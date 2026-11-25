package dev.chieppa.ease.net.view

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import dev.chieppa.ease.ecs.replication.Authority
import dev.chieppa.ease.ecs.replication.BitReader
import dev.chieppa.ease.ecs.replication.NetworkId
import dev.chieppa.ease.ecs.replication.NetworkIdentity
import dev.chieppa.ease.ecs.replication.PeerId
import dev.chieppa.ease.ecs.replication.WireBits
import dev.chieppa.ease.ecs.replication.entityOf
import dev.chieppa.ease.ecs.replication.replication
import dev.chieppa.ease.ecs.replication.spawnEntity
import dev.chieppa.ease.net.channel.Channels

/**
 * Applies the spawns and despawns a client receives from [host], treating every byte as hostile. A
 * spawn builds a replica under the host's network id, held by [host] and owned as the spawn says,
 * from the scene node [content] finds or from the replicated state alone; a despawn removes one. A
 * refused message changes nothing. Each one applied goes in [record], which snapshots are matched
 * against. Only ids the host issues (`NetworkId.isShared`) are taken, so a replica never meets an
 * entity the client made itself. Spawns allocate the replica they build; despawns allocate nothing
 * once the record has grown.
 */
internal class ViewReceiver(
  private val world: World,
  private val host: PeerId,
  private val content: ReplicaContent,
  private val record: ViewRecord,
) {
  private val replication = world.replication
  private val reader = BitReader()
  private val spawns = SpawnReader(replication.types, reader)

  /** Applies the message in the first [length] bytes of [bytes], or returns why it was refused. */
  fun receive(bytes: ByteArray, length: Int): ViewRefusal? {
    val refusal =
      when {
        length < 0 || length > bytes.size -> ViewRefusal.TRUNCATED
        length > Channels.MAX_RELIABLE_BYTES -> ViewRefusal.TOO_LONG
        else -> read(bytes, length)
      }
    return refusal
  }

  private fun read(bytes: ByteArray, length: Int): ViewRefusal? {
    reader.reset(bytes, length)
    spawns.reset()
    when (reader.readBits(ViewMessages.KIND_BITS)) {
      ViewMessages.SPAWN -> spawn()
      ViewMessages.DESPAWN -> despawn()
      else -> spawns.refuse(ViewRefusal.UNKNOWN_KIND)
    }
    return if (spawns.ok) null else spawns.refusal
  }

  private fun despawn() {
    val id = networkId()
    val entity = if (spawns.ok) world.entityOf(id) else Entity.NONE
    when {
      !spawns.ok -> Unit
      entity == Entity.NONE -> spawns.refuse(ViewRefusal.UNKNOWN_ENTITY)
      replication.hasAuthority(entity) -> spawns.refuse(ViewRefusal.LOCAL_AUTHORITY)
      finished() -> {
        world -= entity
        record.despawned(id, entity)
      }
    }
  }

  private fun spawn() {
    val id = networkId()
    val owner = reader.readVarLong()
    val form = if (spawns.ok) spawns.form() else null
    when {
      !spawns.ok -> Unit
      world.entityOf(id) != Entity.NONE -> spawns.refuse(ViewRefusal.DUPLICATE_ENTITY)
      owner !in 0..Int.MAX_VALUE -> spawns.refuse(ViewRefusal.BAD_PEER)
    }
    val base = if (spawns.ok) baseOf(checkNotNull(form)) else null
    val components = base?.let { spawns.components(it) }
    if (components != null && finished()) build(id, PeerId(owner.toInt()), components)
  }

  /** The components the local files give the replica, or null after refusing the form. */
  private fun baseOf(form: SpawnForm): List<Component<out Any>>? =
    when (form) {
      SpawnForm.Plain -> emptyList()
      is SpawnForm.SceneNode -> {
        val placement = form.placement
        val refusal = content.nodeRefusal(form.asset, placement, form.key)
        if (refusal != null) spawns.refuse(refusal)
        if (refusal == null) content.node(form.asset, placement, form.key) else null
      }
    }

  private fun build(id: NetworkId, owner: PeerId, components: List<Component<out Any>>) {
    val entity = world.spawnEntity {
      it += NetworkIdentity(id)
      it += Authority(host, owner)
      it += components
    }
    record.spawned(entity)
  }

  /** Reads a network id, refusing one the host does not issue. */
  private fun networkId(): NetworkId {
    val id = NetworkId(reader.readVarLong())
    if (reader.ok && !id.isShared) spawns.refuse(ViewRefusal.BAD_NETWORK_ID)
    return id
  }

  /** Whether the message ends here, refusing it when more than padding follows. */
  private fun finished(): Boolean {
    if (spawns.ok && reader.remainingBits >= WireBits.BYTE_BITS) {
      spawns.refuse(ViewRefusal.TRAILING_DATA)
    }
    return spawns.ok
  }
}
