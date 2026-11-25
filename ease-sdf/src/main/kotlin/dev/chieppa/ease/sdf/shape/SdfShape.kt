package dev.chieppa.ease.sdf.shape

import kotlin.math.sqrt

/**
 * A solid described by its signed distance: negative inside, positive outside, zero on its surface,
 * in world units. Shapes are immutable trees built from code: primitives centered on the origin,
 * moved, turned and scaled by [Transformed], and joined by [Combined] with an optional blend
 * radius. A tree evaluates identically in Kotlin and in the WGSL generated from it.
 */
public sealed interface SdfShape

/** A ball of [radius]. */
public class Sphere(public val radius: Float) : SdfShape {
  init {
    require(radius > 0f) { "A sphere's radius is positive, got $radius" }
  }
}

/** A box reaching [halfX], [halfY] and [halfZ] from its center along each axis. */
public class Box(public val halfX: Float, public val halfY: Float, public val halfZ: Float) :
  SdfShape {
  init {
    require(halfX > 0f && halfY > 0f && halfZ > 0f) {
      "A box's half sizes are positive, got $halfX, $halfY, $halfZ"
    }
  }
}

/**
 * A box reaching [halfX], [halfY] and [halfZ] from its center, its edges and corners rounded with
 * [radius], which is no more than its smallest half size.
 */
public class RoundedBox(
  public val halfX: Float,
  public val halfY: Float,
  public val halfZ: Float,
  public val radius: Float,
) : SdfShape {
  init {
    require(halfX > 0f && halfY > 0f && halfZ > 0f) {
      "A box's half sizes are positive, got $halfX, $halfY, $halfZ"
    }
    require(radius >= 0f && radius <= minOf(halfX, halfY, halfZ)) {
      "A rounded box's radius is 0 to its smallest half size, got $radius"
    }
  }
}

/** A capsule along y: a segment from -[halfHeight] to [halfHeight], swept by a ball of [radius]. */
public class Capsule(public val halfHeight: Float, public val radius: Float) : SdfShape {
  init {
    require(halfHeight >= 0f && radius > 0f) {
      "A capsule has a half height of 0 or more and a positive radius, got $halfHeight, $radius"
    }
  }
}

/** A flat-ended cylinder along y, [radius] round and reaching [halfHeight] each way. */
public class Cylinder(public val radius: Float, public val halfHeight: Float) : SdfShape {
  init {
    require(radius > 0f && halfHeight > 0f) {
      "A cylinder's radius and half height are positive, got $radius, $halfHeight"
    }
  }
}

/**
 * Everything on the back side of a plane: the solid half-space below the plane whose unit normal is
 * ([normalX], [normalY], [normalZ]), normalized here, at [offset] along that normal from the
 * origin.
 */
public class Plane(normalX: Float, normalY: Float, normalZ: Float, public val offset: Float = 0f) :
  SdfShape {
  private val length = sqrt(normalX * normalX + normalY * normalY + normalZ * normalZ)

  init {
    require(length > 0f) { "A plane's normal has a length" }
  }

  /** The unit normal, pointing out of the solid. */
  public val normalX: Float = normalX / length
  public val normalY: Float = normalY / length
  public val normalZ: Float = normalZ / length
}

/**
 * A ring around y: a circle of [majorRadius] in the x-z plane, swept by a ball of [minorRadius].
 */
public class Torus(public val majorRadius: Float, public val minorRadius: Float) : SdfShape {
  init {
    require(majorRadius > 0f && minorRadius > 0f) {
      "A torus's radii are positive, got $majorRadius, $minorRadius"
    }
  }
}
