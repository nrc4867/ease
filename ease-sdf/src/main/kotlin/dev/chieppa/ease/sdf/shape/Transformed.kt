package dev.chieppa.ease.sdf.shape

import dev.chieppa.ease.math.Quat

/**
 * [shape] scaled by [scale], turned by [rotation], then moved to ([x], [y], [z]): a point of the
 * shape at p sits at translation + rotation(scale * p). Scale is uniform, so distances stay exact.
 */
public class Transformed(
  public val shape: SdfShape,
  public val x: Float = 0f,
  public val y: Float = 0f,
  public val z: Float = 0f,
  rotation: Quat = Quat.IDENTITY,
  public val scale: Float = 1f,
) : SdfShape {
  init {
    require(scale > 0f) { "A shape's scale is positive, got $scale" }
  }

  /** The rotation, normalized. */
  public val rotation: Quat = rotation.normalize()
}
