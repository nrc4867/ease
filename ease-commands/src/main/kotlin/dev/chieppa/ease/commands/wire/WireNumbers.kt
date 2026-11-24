package dev.chieppa.ease.commands.wire

import dev.chieppa.ease.ecs.replication.BitReader
import dev.chieppa.ease.ecs.replication.BitWriter
import dev.chieppa.ease.ecs.replication.DecodeStatus

/** Value bits in each group of an unsigned variable-length number; a continuation bit follows. */
private const val GROUP_BITS = 7

/** Masks a number's lowest group. */
private const val GROUP_MASK = 0x7FL

/** The most groups an unsigned 64-bit number takes. */
private const val MAX_GROUPS = 10

/** The bits the last of [MAX_GROUPS] groups may hold: 64 - 9 * 7. */
private const val LAST_GROUP_MAX = 1L

/** Bits in a `Long`'s sign shift. */
private const val SIGN_SHIFT = 63

/**
 * Writes [value] as an unsigned 64-bit number in groups of seven bits, small values shortest; the
 * full range of `Long` bit patterns is accepted.
 */
internal fun BitWriter.writeUnsigned(value: Long) {
  var rest = value
  while (true) {
    writeBits((rest and GROUP_MASK).toInt(), GROUP_BITS)
    rest = rest ushr GROUP_BITS
    writeBoolean(rest != 0L)
    if (rest == 0L) return
  }
}

/**
 * Reads what [writeUnsigned] wrote. A number longer than ten groups, or whose tenth group holds
 * more than the one bit left of 64, fails [this] as malformed.
 */
internal fun BitReader.readUnsigned(): Long {
  var result = 0L
  for (group in 0 until MAX_GROUPS) {
    val bits = readBits(GROUP_BITS).toLong()
    if (group == MAX_GROUPS - 1 && bits > LAST_GROUP_MAX) break
    result = result or (bits shl (group * GROUP_BITS))
    if (!readBoolean()) return result
  }
  fail(DecodeStatus.MALFORMED)
  return 0L
}

/** [value] with its sign moved to the lowest bit, so small negative numbers stay short. */
internal fun zigzag(value: Long): Long = (value shl 1) xor (value shr SIGN_SHIFT)

/** The number [zigzag] made [value] from. */
internal fun unzigzag(value: Long): Long = (value ushr 1) xor -(value and 1L)
