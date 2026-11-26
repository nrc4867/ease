package dev.chieppa.ease.gltf

/**
 * An accessor read in full: [count] elements of [components] numbers each, packed element after
 * element in [values]. Integer accessors read as floats hold their plain values, or 0 to 1 (-1 to
 * 1) when normalized.
 */
public class FloatValues(
  public val count: Int,
  public val components: Int,
  public val values: FloatArray,
) {
  /** Number [component] of element [index]. */
  public operator fun get(index: Int, component: Int): Float =
    values[index * components + component]
}

/** An integer accessor read in full, such as indices or joints, packed as [FloatValues] is. */
public class IntValues(
  public val count: Int,
  public val components: Int,
  public val values: IntArray,
) {
  /** Number [component] of element [index]. */
  public operator fun get(index: Int, component: Int): Int = values[index * components + component]
}
