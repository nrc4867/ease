package dev.chieppa.ease.sdf.shape

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
