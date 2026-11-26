package dev.chieppa.ease.sdf.gpu

import dev.chieppa.ease.math.Quat
import dev.chieppa.ease.sdf.field.DistanceField
import dev.chieppa.ease.sdf.mesh.SdfGrid
import dev.chieppa.ease.sdf.shape.Box
import dev.chieppa.ease.sdf.shape.Capsule
import dev.chieppa.ease.sdf.shape.Plane
import dev.chieppa.ease.sdf.shape.SdfShape
import dev.chieppa.ease.sdf.shape.Sphere
import dev.chieppa.ease.sdf.shape.intersect
import dev.chieppa.ease.sdf.shape.moved
import dev.chieppa.ease.sdf.shape.rotated
import dev.chieppa.ease.sdf.shape.subtract
import dev.chieppa.ease.sdf.shape.union

/**
 * A cave built from code: a block of rock with a tunnel carved through it. The tunnel runs along x
 * from an opening at x = -14 into a round chamber at x = 4, then turns along -z to an opening at z
 * = -10. Its floor is flat at y = [FLOOR], blended into the walls.
 */
object TestCave {
  const val FLOOR = -2f
  const val CELL = 0.4f
  const val HALF_X = 14f
  const val HALF_Y = 5f
  const val HALF_Z = 10f

  /** The tunnel along x reaches this far from its middle at z = 0, a 3-unit-wide floor. */
  const val TUNNEL_RADIUS = 2.5f

  val shape: SdfShape by lazy {
    // A quarter turn about z lays a capsule along x; one about x lays it along z.
    val alongX = Quat(0f, 0f, QUARTER, QUARTER)
    val alongZ = Quat(QUARTER, 0f, 0f, QUARTER)
    val entrance = Capsule(9f, TUNNEL_RADIUS).rotated(alongX).moved(-7f, 0f, 0f)
    val chamber = Sphere(3.5f).moved(4f, 0.5f, 0f)
    val exit = Capsule(6f, 2.2f).rotated(alongZ).moved(4f, 0f, -6f)
    val passages = entrance.union(chamber, blend = 1f).union(exit, blend = 1f)
    val aboveFloor = Plane(0f, -1f, 0f, offset = -FLOOR)
    val air = passages.intersect(aboveFloor, blend = 0.3f)
    Box(HALF_X, HALF_Y, HALF_Z).subtract(air, blend = 0.5f)
  }

  fun field(): DistanceField = DistanceField(shape)

  fun grid(): SdfGrid = SdfGrid.around(-HALF_X, -HALF_Y, -HALF_Z, HALF_X, HALF_Y, HALF_Z, CELL)

  /** The sine and cosine of an eighth of a turn, a quarter turn's quaternion components. */
  private const val QUARTER = 0.70710677f
}
