package dev.chieppa.ease.codegen.output

/** Field masks as the generated code spells them. */
internal object ComponentMasks {
  const val INT_BITS = 32

  /** The mask with a bit for each of [count] fields. */
  fun all(count: Int): Int = if (count >= INT_BITS) -1 else (1 shl count) - 1

  /**
   * [mask] as a Kotlin `Int` expression. A negative one is spelled through `Int.MIN_VALUE`, since a
   * literal such as `-2147483648` is a `Long` to the compiler.
   */
  fun literal(mask: Int): String =
    if (mask >= 0) mask.toString() else "(Int.MIN_VALUE or ${mask and Int.MAX_VALUE})"
}
