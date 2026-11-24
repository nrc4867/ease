package dev.chieppa.ease.ecs.replication

private const val BYTE_BITS = 8
private const val BYTE_MASK = 7
private const val BYTE_SHIFT = 3
private const val BYTE_VALUE = 0xFF
private const val INT_BITS = 32
private const val VARINT_GROUP_BITS = 7
private const val VARINT_MAX_GROUPS = 10

/**
 * Reads what a [BitWriter] wrote, treating every byte as hostile: a read never runs past the length
 * it was given and never throws. The first problem sets [status], and every read after it returns
 * zero, so a decoder checks [ok] once per step instead of after every value.
 */
public class BitReader {
  private var bytes: ByteArray = ByteArray(0)
  private var limitBits = 0

  /** How many bits have been read. */
  public var bitPosition: Int = 0
    private set

  /** [DecodeStatus.OK], or the first problem met. */
  public var status: DecodeStatus = DecodeStatus.OK
    private set

  /** Whether nothing has gone wrong so far. */
  public val ok: Boolean
    get() = status == DecodeStatus.OK

  /** How many bits are left to read. */
  public val remainingBits: Int
    get() = limitBits - bitPosition

  /** Reads the first [length] bytes of [source] from the start, which must lie inside it. */
  public fun reset(source: ByteArray, length: Int) {
    require(length in 0..source.size) { "Length $length is outside a ${source.size}-byte array" }
    bytes = source
    limitBits = length * BYTE_BITS
    bitPosition = 0
    status = DecodeStatus.OK
  }

  /** Records [problem] unless an earlier one is already recorded. */
  public fun fail(problem: DecodeStatus) {
    if (ok) status = problem
  }

  /** Reads [count] bits, from 0 to 32, as the lowest bits of the result. */
  public fun readBits(count: Int): Int {
    require(count in 0..INT_BITS) { "Can read 0 to 32 bits at once, not $count" }
    if (ok && count > remainingBits) fail(DecodeStatus.TRUNCATED)
    if (!ok) return 0
    var result = 0
    var done = 0
    while (done < count) {
      val offset = bitPosition and BYTE_MASK
      val taken = minOf(BYTE_BITS - offset, count - done)
      val byte = bytes[bitPosition ushr BYTE_SHIFT].toInt() and BYTE_VALUE
      val bits = (byte ushr offset) and ((1 shl taken) - 1)
      result = result or (bits shl done)
      done += taken
      bitPosition += taken
    }
    return result
  }

  /** Reads one bit. */
  public fun readBoolean(): Boolean = readBits(1) != 0

  /** Reads a number [BitWriter.writeVarLong] wrote; one longer than ten groups is malformed. */
  public fun readVarLong(): Long {
    var result = 0L
    for (group in 0 until VARINT_MAX_GROUPS) {
      val bits = readBits(VARINT_GROUP_BITS).toLong()
      result = result or (bits shl (group * VARINT_GROUP_BITS))
      if (!readBoolean()) return result
    }
    fail(DecodeStatus.MALFORMED)
    return 0L
  }
}
