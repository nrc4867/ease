package dev.chieppa.ease.scene.node

import dev.chieppa.ease.annotations.Doc
import kotlinx.serialization.Serializable

/** Hex digits in a local id's text: 64 bits, 4 bits a digit. */
internal const val LOCAL_ID_DIGITS = 16

private const val HEX_RADIX = 16

@Doc(
  "A node's id inside its scene or prefab: 64 random bits, written as 16 lowercase hex digits. " +
    "It is also the name of the node's file, so two branches that each add a node never collide."
)
@Serializable(with = LocalIdSerializer::class)
@JvmInline
public value class LocalId(@Doc("The id's 64 bits.") public val value: Long) : Comparable<LocalId> {
  /** Ids sort as their text does. */
  override fun compareTo(other: LocalId): Int = java.lang.Long.compareUnsigned(value, other.value)

  /** The id as 16 lowercase hex digits, its spelling in files and keys. */
  public val text: String
    get() = java.lang.Long.toUnsignedString(value, HEX_RADIX).padStart(LOCAL_ID_DIGITS, '0')

  override fun toString(): String = text

  public companion object {
    /**
     * The id [text] spells: exactly 16 lowercase hex digits. Throws [IllegalArgumentException]
     * otherwise.
     */
    public fun parse(text: String): LocalId {
      require(text.length == LOCAL_ID_DIGITS && text.all { it in '0'..'9' || it in 'a'..'f' }) {
        "A local id is $LOCAL_ID_DIGITS lowercase hex digits, not \"$text\""
      }
      return LocalId(java.lang.Long.parseUnsignedLong(text, HEX_RADIX))
    }
  }
}
