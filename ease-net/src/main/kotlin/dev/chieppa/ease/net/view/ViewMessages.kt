package dev.chieppa.ease.net.view

import dev.chieppa.ease.ecs.replication.BitReader
import dev.chieppa.ease.ecs.replication.BitWriter
import dev.chieppa.ease.ecs.replication.WireBits

/** The mask of a long's low 32 bits. */
private const val LOW_BITS = 0xFFFF_FFFFL

/**
 * The messages that bring entities into a peer's view and take them out, sent by the host on the
 * reliable ordered channel after its hello, one per message, padded to a byte:
 * - spawn: kind, network id (variable length), owner peer (variable length), form, then for a scene
 *   node the asset GUID (128 bits), the placement depth and key depth (8 bits each) and every id of
 *   the key (64 bits each); then a 9-bit component count and, per replicated component, its 8-bit
 *   type id and its state with only the fields the receiving peer may hold in its change mask;
 * - despawn: kind and network id.
 */
internal object ViewMessages {
  /** Bits a message kind takes. */
  const val KIND_BITS: Int = 8

  /** The kind of a spawn. */
  const val SPAWN: Int = 1

  /** The kind of a despawn. */
  const val DESPAWN: Int = 2

  /** Bits a spawn's form takes. */
  const val FORM_BITS: Int = 8

  /** The form of a [SpawnForm.Plain] spawn. */
  const val PLAIN: Int = 0

  /** The form of a [SpawnForm.SceneNode] spawn. */
  const val SCENE_NODE: Int = 1

  /** Bits a placement or key depth takes. */
  const val DEPTH_BITS: Int = 8

  /** The deepest node key a spawn names; a deeper node goes as [SpawnForm.Plain]. */
  const val MAX_KEY_DEPTH: Int = 64

  /** Writes [value] as 64 bits, low half first. */
  fun writeLong(writer: BitWriter, value: Long) {
    writer.writeBits((value and LOW_BITS).toInt(), WireBits.INT_BITS)
    writer.writeBits((value ushr WireBits.INT_BITS).toInt(), WireBits.INT_BITS)
  }

  /** Reads a long [writeLong] wrote. */
  fun readLong(reader: BitReader): Long {
    val low = reader.readBits(WireBits.INT_BITS).toLong() and LOW_BITS
    val high = reader.readBits(WireBits.INT_BITS).toLong()
    return low or (high shl WireBits.INT_BITS)
  }
}
