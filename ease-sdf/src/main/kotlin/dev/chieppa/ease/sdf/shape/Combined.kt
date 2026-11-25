package dev.chieppa.ease.sdf.shape

/** How [Combined] joins its two shapes. */
public enum class CsgOp {
  /** Everything in either shape. */
  UNION,

  /** The first shape with the second carved out of it. */
  SUBTRACT,

  /** Only what is in both shapes. */
  INTERSECT,
}

/**
 * [first] and [second] joined by [op]. With a [blend] radius above zero the seam is smoothed over
 * about that distance: a fillet for a union, a rounded cut for a subtraction or an intersection.
 */
public class Combined(
  public val op: CsgOp,
  public val first: SdfShape,
  public val second: SdfShape,
  public val blend: Float = 0f,
) : SdfShape {
  init {
    require(blend >= 0f) { "A blend radius is 0 or more, got $blend" }
  }
}
