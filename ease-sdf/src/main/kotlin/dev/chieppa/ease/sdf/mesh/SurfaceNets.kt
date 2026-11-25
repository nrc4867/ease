package dev.chieppa.ease.sdf.mesh

import dev.chieppa.ease.sdf.field.DistanceField
import kotlin.math.sqrt

/**
 * Surface nets on the CPU, the reference the GPU mesher matches and the source of colliders, since
 * Kotlin's float arithmetic gives the same bits on every machine. Corners with a negative distance
 * are inside. Every cell with corners on both sides gets one vertex, the average of where its edges
 * cross the surface, with the field's normalized central-difference gradient as its normal. Every
 * grid edge crossing the surface, away from the grid's border, joins the four cells around it in a
 * quad of two triangles facing outward. Vertices come in corner order (x fastest, then y, then z),
 * quads in corner order with a corner's x, y and z edges in turn, as the GPU writes them.
 */
public object SurfaceNets {
  /** The surface of [field] on [grid]. Allocates as it goes: meshing is load-time work. */
  public fun mesh(field: DistanceField, grid: SdfGrid): SdfMesh {
    val corner = IntArray(AXES)
    val samples = FloatArray(grid.pointCount)
    for (index in 0 until grid.pointCount) {
      grid.cornerOf(index, corner)
      samples[index] =
        field.distance(
          coordinate(grid, X, corner[X]),
          coordinate(grid, Y, corner[Y]),
          coordinate(grid, Z, corner[Z]),
        )
    }
    val edges = CellEdges(grid, samples)
    val positions = FloatList()
    val normals = FloatList()
    val vertex = FloatArray(AXES)
    val normal = FloatArray(AXES)
    for (index in 0 until grid.pointCount) {
      grid.cornerOf(index, corner)
      if (!grid.isCell(corner) || !edges.vertex(corner, vertex)) continue
      edges.vertexOf[index] = positions.size / AXES
      gradient(field, grid.cellSize * NORMAL_STEP, vertex, normal)
      positions.add(vertex)
      normals.add(normal)
    }
    val indices = IntList()
    val quad = IntArray(QUAD_INDICES)
    for (index in 0 until grid.pointCount) {
      grid.cornerOf(index, corner)
      for (axis in 0 until AXES) if (edges.quad(corner, axis, quad)) indices.add(quad)
    }
    return SdfMesh(positions.toArray(), normals.toArray(), indices.toArray())
  }

  /** The unit gradient of [field] at [at] by central differences [step] apart, into [into]. */
  private fun gradient(field: DistanceField, step: Float, at: FloatArray, into: FloatArray) {
    val x = at[X]
    val y = at[Y]
    val z = at[Z]
    val gx = field.distance(x + step, y, z) - field.distance(x - step, y, z)
    val gy = field.distance(x, y + step, z) - field.distance(x, y - step, z)
    val gz = field.distance(x, y, z + step) - field.distance(x, y, z - step)
    val length = sqrt(gx * gx + gy * gy + gz * gz)
    if (length > 0f) {
      into[X] = gx / length
      into[Y] = gy / length
      into[Z] = gz / length
    } else {
      into[X] = 0f
      into[Y] = 1f
      into[Z] = 0f
    }
  }

  /** The world coordinate along [axis] of corner [index]. */
  internal fun coordinate(grid: SdfGrid, axis: Int, index: Int): Float {
    val origin =
      when (axis) {
        X -> grid.originX
        Y -> grid.originY
        else -> grid.originZ
      }
    return origin + grid.cellSize * index.toFloat()
  }

  /** Central differences reach this fraction of a cell either side of a vertex. */
  internal const val NORMAL_STEP = 0.5f
  internal const val NONE = -1
  internal const val AXES = 3
  internal const val X = 0
  internal const val Y = 1
  internal const val Z = 2
  private const val QUAD_INDICES = 6
}
