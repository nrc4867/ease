package dev.chieppa.ease.scene.node

import dev.chieppa.ease.annotations.Doc
import kotlinx.serialization.Serializable

/** The lowest digit, which never ends a key, so a key always fits before any other. */
private const val LOWEST = 'a'

/** One past the highest digit. */
private const val DIGITS = 26

@Doc(
  "Where a node sits among its siblings: a string of the letters a to z, compared as text. A key " +
    "always fits between any two others (between b and c is bn), so inserting or moving a node " +
    "changes only that node's file. Siblings with equal keys sort by local id."
)
@Serializable
@JvmInline
public value class OrderKey(@Doc("The key's letters.") public val text: String) :
  Comparable<OrderKey> {
  init {
    require(text.isNotEmpty() && text.all { it in 'a'..'z' } && text.last() != LOWEST) {
      "An order key is letters a to z not ending in a, not \"$text\""
    }
  }

  override fun compareTo(other: OrderKey): Int = text.compareTo(other.text)

  override fun toString(): String = text

  public companion object {
    /** The key of a first child, in the middle of the range so either side has room. */
    public val FIRST: OrderKey = between(null, null)

    /**
     * A key strictly between [before] and [after]; null means the start or the end of the siblings.
     * Throws [IllegalArgumentException] when [before] is not below [after].
     */
    public fun between(before: OrderKey?, after: OrderKey?): OrderKey {
      require(before == null || after == null || before < after) {
        "No key fits between $before and $after"
      }
      return OrderKey(midpoint(before?.text.orEmpty(), after?.text))
    }

    /** The key after [last], or [FIRST] when there are no siblings. */
    public fun after(last: OrderKey?): OrderKey = between(last, null)

    /**
     * A string strictly between [low] (empty for the start) and [high] (null for the end), neither
     * ending in the lowest digit. Digits run a to z; a missing digit of [low] counts as the lowest.
     */
    private fun midpoint(low: String, high: String?): String {
      if (high != null) {
        var shared = 0
        while (shared < high.length && digitAt(low, shared) == high[shared] - LOWEST) shared++
        if (shared > 0) {
          return high.substring(0, shared) + midpoint(low.drop(shared), high.substring(shared))
        }
      }
      val lowDigit = digitAt(low, 0)
      val highDigit = if (high == null) DIGITS else high[0] - LOWEST
      return when {
        highDigit - lowDigit > 1 -> digit((lowDigit + highDigit + 1) / 2)
        high != null && high.length > 1 -> high.substring(0, 1)
        else -> digit(lowDigit) + midpoint(low.drop(1), null)
      }
    }

    private fun digitAt(text: String, index: Int): Int =
      if (index < text.length) text[index] - LOWEST else 0

    private fun digit(value: Int): String = (LOWEST + value).toString()
  }
}
