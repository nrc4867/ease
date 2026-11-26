package dev.chieppa.ease.sdf.mesh

import kotlin.math.ceil

/**
 * The lattice a distance field is meshed on: cubes of [cellSize], [cellsX] by [cellsY] by [cellsZ]
 * of them, whose lowest corner is at ([originX], [originY], [originZ]). The field is sampled at
 * every cell corner; a surface the grid does not enclose stays open at its edges.
 */
@Suppress("LongParameterList") // A lattice's corner, cell size and three cell counts.
public class SdfGrid(
  public val originX: Float,
  public val originY: Float,
  public val originZ: Float,
  public val cellSize: Float,
  public val cellsX: Int,
  public val cellsY: Int,
  public val cellsZ: Int,
) {
  init {
    require(cellSize > 0f) { "Cells have a positive size, got $cellSize" }
    require(cellsX > 0 && cellsY > 0 && cellsZ > 0) {
      "A grid has cells along every axis, got $cellsX x $cellsY x $cellsZ"
    }
    val corners = (cellsX + 1.0) * (cellsY + 1.0) * (cellsZ + 1.0)
    require(corners <= MAX_POINTS) {
      "$cellsX x $cellsY x $cellsZ cells have more than $MAX_POINTS corners; mesh it in parts"
    }
  }

  /** Corners along each axis, one more than the cells. */
  public val pointsX: Int
    get() = cellsX + 1

  public val pointsY: Int
    get() = cellsY + 1

  public val pointsZ: Int
    get() = cellsZ + 1

  /** Every cell corner, the field's samples. */
  public val pointCount: Int
    get() = pointsX * pointsY * pointsZ

  /** The index of the corner at ([x], [y], [z]): x varies fastest, then y, then z. */
  public fun point(x: Int, y: Int, z: Int): Int = x + pointsX * (y + pointsY * z)

  /** Writes corner [index]'s x, y and z indices into [into]. */
  internal fun cornerOf(index: Int, into: IntArray) {
    into[0] = index % pointsX
    into[1] = index / pointsX % pointsY
    into[2] = index / (pointsX * pointsY)
  }

  /** Whether [corner] is the lowest corner of a cell, rather than on the grid's far faces. */
  internal fun isCell(corner: IntArray): Boolean =
    corner[0] < cellsX && corner[1] < cellsY && corner[2] < cellsZ

  /** The world coordinate along [axis] (0 x, 1 y, 2 z) of the corners numbered [index] on it. */
  public fun coordinate(axis: Int, index: Int): Float {
    val origin =
      when (axis) {
        0 -> originX
        1 -> originY
        else -> originZ
      }
    return origin + cellSize * index.toFloat()
  }

  public companion object {
    /** The most corners a grid may have, as many as an array holds. */
    public const val MAX_POINTS: Int = Int.MAX_VALUE - 8

    /**
     * The grid of [cellSize] cells covering the box from ([minX], [minY], [minZ]) to ([maxX],
     * [maxY], [maxZ]) with a cell to spare on every side, so a shape inside the box is closed.
     */
    @Suppress("LongParameterList") // A box's two corners and the cell size.
    public fun around(
      minX: Float,
      minY: Float,
      minZ: Float,
      maxX: Float,
      maxY: Float,
      maxZ: Float,
      cellSize: Float,
    ): SdfGrid {
      fun cells(min: Float, max: Float) = ceil((max - min) / cellSize).toInt() + 2
      return SdfGrid(
        minX - cellSize,
        minY - cellSize,
        minZ - cellSize,
        cellSize,
        cells(minX, maxX),
        cells(minY, maxY),
        cells(minZ, maxZ),
      )
    }
  }
}
