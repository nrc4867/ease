package dev.chieppa.ease.net.view

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import dev.chieppa.ease.ecs.replication.BitWriter
import dev.chieppa.ease.ecs.replication.BitWriterFull
import dev.chieppa.ease.ecs.replication.PeerId
import dev.chieppa.ease.ecs.replication.ReplicatedFields
import dev.chieppa.ease.ecs.replication.ReplicatedTypes
import dev.chieppa.ease.ecs.replication.StatePackets
import dev.chieppa.ease.ecs.replication.replication
import dev.chieppa.ease.net.channel.Channels

/**
 * Writes the host's spawn and despawn messages (see [ViewMessages]) into one reused buffer, for
 * every peer in turn. A spawn holds the entity's current replicated state, limited to the fields
 * its receiver may hold. Despawns allocate nothing; a spawn allocates only what [naming] makes.
 */
internal class SpawnWriter(private val world: World, private val naming: SpawnNaming) {
  private val replication = world.replication
  private val types = replication.types
  private val writer = BitWriter(Channels.MAX_RELIABLE_BYTES)

  /** The bytes of the last message written. */
  val buffer: ByteArray
    get() = writer.buffer

  /** Writes the despawn of the entity [networkId] and returns its length. */
  fun despawn(networkId: Long): Int {
    writer.reset()
    writer.writeBits(ViewMessages.DESPAWN, ViewMessages.KIND_BITS)
    writer.writeVarLong(networkId)
    writer.padToByte()
    return writer.byteLength
  }

  /**
   * Writes the spawn of [entity] under [networkId], owned by [owner], for a peer that owns it or
   * not as [toOwner] says, and returns its length; or returns [NO_FIT] when it is longer than a
   * reliable message, which a debug build throws on instead.
   */
  fun spawn(entity: Entity, networkId: Long, owner: PeerId, toOwner: Boolean): Int {
    writer.reset()
    writer.writeBits(ViewMessages.SPAWN, ViewMessages.KIND_BITS)
    writer.writeVarLong(networkId)
    writer.writeVarLong(owner.value.toLong())
    return try {
      writeForm(naming.formOf(world, entity))
      writeComponents(entity, toOwner)
      writer.padToByte()
      writer.byteLength
    } catch (full: BitWriterFull) {
      check(!replication.debugChecks) { "The spawn of entity #$networkId is too long: $full" }
      NO_FIT
    }
  }

  private fun writeForm(form: SpawnForm) {
    when (form) {
      SpawnForm.Plain -> writer.writeBits(ViewMessages.PLAIN, ViewMessages.FORM_BITS)
      is SpawnForm.SceneNode -> {
        writer.writeBits(ViewMessages.SCENE_NODE, ViewMessages.FORM_BITS)
        ViewMessages.writeLong(writer, form.asset.high)
        ViewMessages.writeLong(writer, form.asset.low)
        writer.writeBits(form.placementDepth, ViewMessages.DEPTH_BITS)
        writer.writeBits(form.key.depth, ViewMessages.DEPTH_BITS)
        for (index in 0 until form.key.depth) {
          ViewMessages.writeLong(writer, form.key.idAt(index).value)
        }
      }
    }
  }

  private fun writeComponents(entity: Entity, toOwner: Boolean) {
    val countAt = writer.bitPosition
    writer.writeBits(0, StatePackets.COMPONENT_COUNT_BITS)
    var count = 0
    for (typeId in 0 until types.size) {
      val type = types[typeId]
      if (!type.isOn(world, entity)) continue
      val serializer = type.serializer
      val audience = if (toOwner) serializer.ownerFields else serializer.othersFields
      val fields = finiteFields(entity, typeId, ReplicatedFields.allFields(serializer.fieldCount))
      writer.writeBits(typeId, ReplicatedTypes.ID_BITS)
      type.writeFrom(world, entity, writer, fields and audience)
      count++
    }
    writer.writeBitsAt(countAt, count, StatePackets.COMPONENT_COUNT_BITS)
  }

  /**
   * [fields] without those of [entity]'s component [typeId] that hold a float that is not finite,
   * so the replica keeps its default there; a debug build throws, naming the field.
   */
  private fun finiteFields(entity: Entity, typeId: Int, fields: Int): Int {
    val type = types[typeId]
    var left = fields
    var bad = type.nonFiniteField(world, entity, left)
    while (bad >= 0) {
      check(!replication.debugChecks) {
        "Entity #${replication.networkIdOf(entity).value} holds a value that is not finite in " +
          "${type.serializer.name}.${type.serializer.fieldNames[bad]}"
      }
      left = left and (1 shl bad).inv()
      bad = type.nonFiniteField(world, entity, left)
    }
    return left
  }

  companion object {
    /** What [spawn] returns for an entity whose spawn does not fit a reliable message. */
    const val NO_FIT: Int = -1
  }
}
