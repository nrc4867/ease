package dev.chieppa.ease.sdf.gpu

import dev.chieppa.ease.sdf.field.DistanceField
import dev.chieppa.ease.sdf.mesh.SdfGrid
import dev.chieppa.ease.sdf.mesh.SdfMesh
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.sqrt

/**
 * Compares a GPU mesh with the CPU's on the same field and grid. Where every corner of a cell is
 * clearly on one side of the surface the two must agree; where a corner's distance is within
 * [ambiguity] of zero, rounding may put it on either side on another GPU, so a vertex or triangle
 * missing near such a corner is allowed.
 */
class MeshMatch(
  private val field: DistanceField,
  private val grid: SdfGrid,
  private val cpu: SdfMesh,
  private val ambiguity: Float,
) {
  /** Cells with a corner whose distance is within [ambiguity] of zero, by corner index. */
  private val ambiguousCells = BooleanArray(grid.pointCount)

  init {
    for (corner in 0 until grid.pointCount) {
      val (x, y, z) = coordsOf(corner)
      val d = field.distance(grid.coordinate(0, x), grid.coordinate(1, y), grid.coordinate(2, z))
      if (abs(d) < ambiguity) markAround(x, y, z)
    }
  }

  /** Whether no corner is within the ambiguity of zero, so the meshes must match exactly. */
  val clear: Boolean
    get() = ambiguousCells.none { it }

  /** CPU vertices by the cell they lie in, for finding a GPU vertex's twin. */
  private val cpuByCell = HashMap<Int, MutableList<Int>>()

  init {
    for (vertex in 0 until cpu.vertexCount) {
      cpuByCell.getOrPut(cellOf(cpu.positions, vertex)) { ArrayList() }.add(vertex)
    }
  }

  /**
   * The problems found comparing [positions], [normals] and [indices] read back from the GPU:
   * vertices or triangles that differ away from ambiguous cells. Empty when they match.
   */
  fun problems(
    positions: FloatArray,
    normals: FloatArray,
    indices: IntArray,
    positionTolerance: Float,
    normalTolerance: Float,
  ): List<String> {
    val problems = ArrayList<String>()
    val twin = IntArray(positions.size / 3) { -1 }
    for (vertex in twin.indices) {
      val match =
        if (vertex < cpu.vertexCount && gap(positions, vertex, vertex) <= positionTolerance) vertex
        else nearestCpu(positions, vertex, positionTolerance)
      if (match < 0) {
        if (!ambiguous(positions, vertex)) problems += "GPU vertex $vertex has no CPU twin"
        continue
      }
      twin[vertex] = match
      for (axis in 0 until 3) {
        val gap = abs(normals[vertex * 3 + axis] - cpu.normals[match * 3 + axis])
        if (gap > normalTolerance) problems += "GPU vertex $vertex's normal is $gap off"
      }
    }
    val matched = twin.filter { it >= 0 }.toSet()
    for (vertex in 0 until cpu.vertexCount) {
      if (vertex !in matched && !ambiguous(cpu.positions, vertex)) {
        problems += "CPU vertex $vertex has no GPU twin"
      }
    }
    val cpuTriangles = triangles(cpu.indices) { it }
    for (triangle in 0 until indices.size / 3) {
      val corners = (0 until 3).map { indices[triangle * 3 + it] }
      val mapped = corners.joinToString(",") { "${twin[it]}" }
      val near = corners.any { ambiguous(positions, it) }
      if (mapped !in cpuTriangles && !near) problems += "GPU triangle $triangle is not the CPU's"
    }
    return problems
  }

  private fun triangles(indices: IntArray, vertex: (Int) -> Int): Set<String> =
    (0 until indices.size / 3)
      .map { t -> (0 until 3).joinToString(",") { corner -> "${vertex(indices[t * 3 + corner])}" } }
      .toSet()

  private fun nearestCpu(positions: FloatArray, vertex: Int, tolerance: Float): Int {
    var best = -1
    var bestGap = tolerance
    for (cell in neighbours(cellOf(positions, vertex))) {
      for (candidate in cpuByCell[cell].orEmpty()) {
        val gap = gap(positions, vertex, candidate)
        if (gap <= bestGap) {
          best = candidate
          bestGap = gap
        }
      }
    }
    return best
  }

  private fun gap(positions: FloatArray, vertex: Int, candidate: Int): Float {
    var sum = 0f
    for (axis in 0 until 3) {
      val d = positions[vertex * 3 + axis] - cpu.positions[candidate * 3 + axis]
      sum += d * d
    }
    return sqrt(sum)
  }

  private fun ambiguous(positions: FloatArray, vertex: Int): Boolean =
    neighbours(cellOf(positions, vertex)).any { it in ambiguousCells.indices && ambiguousCells[it] }

  private fun cellOf(positions: FloatArray, vertex: Int): Int {
    fun at(axis: Int, origin: Float, cells: Int) =
      floor((positions[vertex * 3 + axis] - origin) / grid.cellSize).toInt().coerceIn(0, cells - 1)
    return grid.point(
      at(0, grid.originX, grid.cellsX),
      at(1, grid.originY, grid.cellsY),
      at(2, grid.originZ, grid.cellsZ),
    )
  }

  private fun coordsOf(corner: Int): Triple<Int, Int, Int> =
    Triple(
      corner % grid.pointsX,
      corner / grid.pointsX % grid.pointsY,
      corner / (grid.pointsX * grid.pointsY),
    )

  /** The corners from [low] to [high] steps from [cell]'s along each axis, inside the grid. */
  private fun around(cell: Int, low: Int, high: Int): List<Int> {
    val (x, y, z) = coordsOf(cell)
    val span = high - low + 1
    return (0 until span * span * span).mapNotNull { step ->
      val nx = x + low + step % span
      val ny = y + low + step / span % span
      val nz = z + low + step / (span * span)
      val inside = nx in 0..grid.cellsX && ny in 0..grid.cellsY && nz in 0..grid.cellsZ
      if (inside) grid.point(nx, ny, nz) else null
    }
  }

  private fun neighbours(cell: Int): List<Int> = around(cell, -1, 1)

  /** Marks every cell that has corner ([x], [y], [z]) as ambiguous. */
  private fun markAround(x: Int, y: Int, z: Int) {
    around(grid.point(x, y, z), -1, 0).forEach { ambiguousCells[it] = true }
  }
}
