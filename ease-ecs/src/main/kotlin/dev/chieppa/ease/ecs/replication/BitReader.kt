package dev.chieppa.ease.ecs.replication

/**
 * Reads what a [BitWriter] wrote, treating every byte as hostile: a read never runs past the length
 * it was given and never throws. The first problem sets [status], and every read after it returns
 * zero, so a decoder checks [ok] once per step instead of after every value.
 */
public class BitReader {
  private var bytes: ByteArray = ByteArray(0)
  private var startBits = 0
  private var limitBits = 0
  private var cursor = 0

  /** How many bits have been read. */
  public val bitPosition: Int
    get() = cursor - startBits

  /** [DecodeStatus.OK], or the first problem met. */
  public var status: DecodeStatus = DecodeStatus.OK
    private set

  /** Whether nothing has gone wrong so far. */
  public val ok: Boolean
    get() = status == DecodeStatus.OK

  /** How many bits are left to read. */
  public val remainingBits: Int
    get() = limitBits - cursor

  /** Reads the first [length] bytes of [source] from the start, which must lie inside it. */
  public fun reset(source: ByteArray, length: Int) {
    reset(source, 0, length)
  }

  /** Reads the [length] bytes of [source] from [offset], which must lie inside it. */
  public fun reset(source: ByteArray, offset: Int, length: Int) {
    require(offset >= 0 && length >= 0 && offset <= source.size - length) {
      "$length bytes from $offset are outside a ${source.size}-byte array"
    }
    bytes = source
    startBits = offset * WireBits.BYTE_BITS
    limitBits = (offset + length) * WireBits.BYTE_BITS
    cursor = startBits
    status = DecodeStatus.OK
  }

  /** Records [problem] unless an earlier one is already recorded. */
  public fun fail(problem: DecodeStatus) {
    if (ok) status = problem
  }

  /** Reads [count] bits, from 0 to 32, as the lowest bits of the result. */
  public fun readBits(count: Int): Int {
    require(count in 0..WireBits.INT_BITS) { "Can read 0 to 32 bits at once, not $count" }
    if (ok && count > remainingBits) fail(DecodeStatus.TRUNCATED)
    if (!ok) return 0
    var result = 0
    var done = 0
    while (done < count) {
      val offset = cursor and WireBits.BYTE_MASK
      val taken = minOf(WireBits.BYTE_BITS - offset, count - done)
      val byte = bytes[cursor ushr WireBits.BYTE_SHIFT].toInt() and WireBits.BYTE_VALUE
      val bits = (byte ushr offset) and ((1 shl taken) - 1)
      result = result or (bits shl done)
      done += taken
      cursor += taken
    }
    return result
  }

  /** Reads one bit. */
  public fun readBoolean(): Boolean = readBits(1) != 0

  /** Reads a number [BitWriter.writeVarLong] wrote; one longer than ten groups is malformed. */
  public fun readVarLong(): Long {
    var result = 0L
    for (group in 0 until WireBits.VARINT_MAX_GROUPS) {
      val bits = readBits(WireBits.VARINT_GROUP_BITS).toLong()
      result = result or (bits shl (group * WireBits.VARINT_GROUP_BITS))
      if (!readBoolean()) return result
    }
    fail(DecodeStatus.MALFORMED)
    return 0L
  }
}
