package dev.chieppa.ease.sdf.shape

import dev.chieppa.ease.math.Quat
import dev.chieppa.ease.math.Vec3

/** This shape and [other] together, the seam smoothed over [blend]. */
public fun SdfShape.union(other: SdfShape, blend: Float = 0f): SdfShape =
  Combined(CsgOp.UNION, this, other, blend)

/** This shape with [other] carved out, the cut's edges rounded over [blend]. */
public fun SdfShape.subtract(other: SdfShape, blend: Float = 0f): SdfShape =
  Combined(CsgOp.SUBTRACT, this, other, blend)

/** Only what this shape and [other] share, the edges rounded over [blend]. */
public fun SdfShape.intersect(other: SdfShape, blend: Float = 0f): SdfShape =
  Combined(CsgOp.INTERSECT, this, other, blend)

/** This shape moved by ([x], [y], [z]). */
public fun SdfShape.moved(x: Float, y: Float, z: Float): SdfShape =
  if (this is Transformed) {
    Transformed(shape, this.x + x, this.y + y, this.z + z, rotation, scale)
  } else {
    Transformed(this, x, y, z)
  }

/** This shape turned by [rotation] about the origin. */
public fun SdfShape.rotated(rotation: Quat): SdfShape =
  if (this is Transformed) {
    val moved = rotation.normalize().rotate(Vec3(x, y, z))
    Transformed(shape, moved.x(), moved.y(), moved.z(), rotation.mul(this.rotation), scale)
  } else {
    Transformed(this, rotation = rotation)
  }

/** This shape scaled by [factor] about the origin. */
public fun SdfShape.scaled(factor: Float): SdfShape =
  if (this is Transformed) {
    Transformed(shape, x * factor, y * factor, z * factor, rotation, scale * factor)
  } else {
    Transformed(this, scale = factor)
  }
