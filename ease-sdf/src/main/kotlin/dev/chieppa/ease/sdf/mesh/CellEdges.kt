package dev.chieppa.ease.sdf.mesh

import dev.chieppa.ease.sdf.SdfConstants.AXES
import dev.chieppa.ease.sdf.mesh.SurfaceNets.NONE

/**
 * The cell and edge rules of surface nets over one grid's [samples], as the GPU kernels'
 * `edges.wgsl` and `positions.wgsl` apply them: which cells get a vertex and where, and which edges
 * make a quad of which cells.
 */
internal class CellEdges(private val grid: SdfGrid, private val samples: FloatArray) {
  /** Each cell's vertex index by its lowest corner's index, or [NONE] for a cell with none. */
  val vertexOf = IntArray(grid.pointCount) { NONE }

  /**
   * Whether the cell whose lowest corner is [cell], inside the grid, crosses the surface, and if so
   * its vertex into [into]: the average of where its edges cross.
   */
  fun vertex(cell: IntArray, into: FloatArray): Boolean {
    var sumX = 0f
    var sumY = 0f
    var sumZ = 0f
    var crossings = 0
    for (edge in EDGE_FROM.indices) {
      val from = EDGE_FROM[edge]
      val to = EDGE_TO[edge]
      val a = samples[cornerIndex(cell, from)]
      val b = samples[cornerIndex(cell, to)]
      if (inside(a) == inside(b)) continue
      val t = a / (a - b)
      sumX += crossing(0, cell[0] + bit(from, 0), cell[0] + bit(to, 0), t)
      sumY += crossing(1, cell[1] + bit(from, 1), cell[1] + bit(to, 1), t)
      sumZ += crossing(Z_BIT, cell[Z_BIT] + bit(from, Z_BIT), cell[Z_BIT] + bit(to, Z_BIT), t)
      crossings++
    }
    if (crossings == 0) return false
    val count = crossings.toFloat()
    into[0] = sumX / count
    into[1] = sumY / count
    into[Z_BIT] = sumZ / count
    return true
  }

  /**
   * Whether the edge from corner [corner] along [axis] crosses the surface with four cells around
   * it, and if so its two triangles' six vertex indices into [into], facing out of the solid.
   */
  fun quad(corner: IntArray, axis: Int, into: IntArray): Boolean {
    if (!crosses(corner, axis)) return false
    val u = (axis + 1) % AXES
    val v = (axis + 2) % AXES
    val here = grid.point(corner[0], corner[1], corner[Z_BIT])
    val v00 = vertexOf[here - step(u) - step(v)]
    val v10 = vertexOf[here - step(v)]
    val v11 = vertexOf[here]
    val v01 = vertexOf[here - step(u)]
    check(v00 != NONE && v10 != NONE && v11 != NONE && v01 != NONE) {
      "A crossed edge's cell has no vertex"
    }
    if (inside(samples[here])) fill(into, v00, v10, v11, v01) else fill(into, v00, v01, v11, v10)
    return true
  }

  /**
   * Whether the edge from [corner] along [axis] has four cells around it and crosses the surface.
   */
  private fun crosses(corner: IntArray, axis: Int): Boolean {
    val u = (axis + 1) % AXES
    val v = (axis + 2) % AXES
    val here = grid.point(corner[0], corner[1], corner[Z_BIT])
    return corner[axis] < cells(axis) &&
      corner[u] in 1 until cells(u) &&
      corner[v] in 1 until cells(v) &&
      inside(samples[here]) != inside(samples[here + step(axis)])
  }

  @Suppress("MagicNumber") // A quad's six index slots.
  private fun fill(into: IntArray, a: Int, b: Int, c: Int, d: Int) {
    into[0] = a
    into[1] = b
    into[2] = c
    into[3] = a
    into[4] = c
    into[5] = d
  }

  private fun crossing(axis: Int, from: Int, to: Int, t: Float): Float {
    val a = grid.coordinate(axis, from)
    val b = grid.coordinate(axis, to)
    return a + (b - a) * t
  }

  private fun bit(corner: Int, axis: Int): Int = (corner shr axis) and 1

  private fun cornerIndex(cell: IntArray, corner: Int): Int =
    grid.point(cell[0] + bit(corner, 0), cell[1] + bit(corner, 1), cell[Z_BIT] + bit(corner, Z_BIT))

  private fun cells(axis: Int): Int =
    when (axis) {
      0 -> grid.cellsX
      1 -> grid.cellsY
      else -> grid.cellsZ
    }

  private fun step(axis: Int): Int =
    when (axis) {
      0 -> 1
      1 -> grid.pointsX
      else -> grid.pointsX * grid.pointsY
    }

  private companion object {
    /** Each cell edge's two corners, by corner number: bit 0 is +x, bit 1 +y, bit 2 +z. */
    val EDGE_FROM = intArrayOf(0, 2, 4, 6, 0, 1, 4, 5, 0, 1, 2, 3)
    val EDGE_TO = intArrayOf(1, 3, 5, 7, 2, 3, 6, 7, 4, 5, 6, 7)
    const val Z_BIT = 2

    fun inside(distance: Float): Boolean = distance < 0f
  }
}
