package dev.chieppa.ease.assets

import dev.chieppa.ease.annotations.Doc
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.UUID
import kotlinx.serialization.Serializable

private const val VERSION_FIVE = 0x5000L
private const val VERSION_MASK = 0xf000L
private const val VARIANT_CLEAR = 0x3fffffffffffffffL
private const val VARIANT_RFC = Long.MIN_VALUE
private const val UUID_BYTES = 16

/**
 * The identity of an asset: 128 bits written as a UUID. A source file's GUID is random (version 4)
 * and lives in its meta file; a sub-asset's GUID comes from its parent's and its name (version 5),
 * so it is the same on every machine without a file of its own. Two GUIDs are equal when their bits
 * are, and comparing or hashing one never allocates.
 */
@Doc("An asset's identity: a UUID, written in its usual hyphenated lowercase form.")
@Serializable(with = GuidSerializer::class)
public class Guid(
  @Doc("The first 64 bits of the UUID.") public val high: Long,
  @Doc("The last 64 bits of the UUID.") public val low: Long,
) {
  /**
   * The GUID of the sub-asset called [name] under this one: a version 5 UUID, the same for the same
   * parent and name always.
   */
  public fun named(name: String): Guid {
    val digest = MessageDigest.getInstance("SHA-1")
    digest.update(ByteBuffer.allocate(UUID_BYTES).putLong(high).putLong(low).array())
    val hash = ByteBuffer.wrap(digest.digest(name.toByteArray(Charsets.UTF_8)))
    val first = (hash.getLong(0) and VERSION_MASK.inv()) or VERSION_FIVE
    val second = (hash.getLong(Long.SIZE_BYTES) and VARIANT_CLEAR) or VARIANT_RFC
    return Guid(first, second)
  }

  override fun equals(other: Any?): Boolean =
    other is Guid && other.high == high && other.low == low

  override fun hashCode(): Int = (high xor low).hashCode()

  override fun toString(): String = UUID(high, low).toString()

  public companion object {
    /** A new random (version 4) GUID. */
    public fun random(): Guid = of(UUID.randomUUID())

    /** The GUID written as [text]. Throws [IllegalArgumentException] when it is not a UUID. */
    public fun parse(text: String): Guid = of(UUID.fromString(text))

    private fun of(uuid: UUID): Guid = Guid(uuid.mostSignificantBits, uuid.leastSignificantBits)
  }
}
