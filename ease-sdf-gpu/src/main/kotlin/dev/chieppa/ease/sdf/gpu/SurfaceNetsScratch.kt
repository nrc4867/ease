package dev.chieppa.ease.sdf.gpu

import dev.chieppa.ease.gpu.GpuContext
import dev.chieppa.ease.gpu.compute.BufferUse
import dev.chieppa.ease.gpu.compute.GpuBuffer
import dev.chieppa.ease.memory.NativeWriter
import dev.chieppa.ease.sdf.SdfConstants.AXES
import dev.chieppa.ease.sdf.mesh.SdfGrid
import java.lang.foreign.Arena
import java.lang.foreign.ValueLayout

/** Bytes of one `vec2u`: a vertex count and a quad count. */
private const val PAIR_BYTES = 8L

/**
 * The buffers the surface nets passes share on [context], filled with [grid]'s numbers and corner
 * coordinates and [shader]'s node parameters, and freed once the mesh is written.
 */
internal class SurfaceNetsScratch(
  context: GpuContext,
  grid: SdfGrid,
  shader: SdfShader,
  blocks: Int,
) : AutoCloseable {
  private val points = grid.pointCount.toLong()
  val grid = GpuBuffer(context, SurfaceNetsGridWriter.STRUCT_SIZE, BufferUse.UNIFORM)
  val nodes = GpuBuffer(context, shader.nodeBytes, BufferUse.STORAGE)
  val axes = GpuBuffer(context, axisBytes(grid), BufferUse.STORAGE)
  val samples = GpuBuffer(context, points * Float.SIZE_BYTES, BufferUse.STORAGE)
  val counts = GpuBuffer(context, points * PAIR_BYTES, BufferUse.STORAGE)
  val offsets = GpuBuffer(context, points * PAIR_BYTES, BufferUse.STORAGE)
  val blockSums = GpuBuffer(context, blocks * PAIR_BYTES, BufferUse.STORAGE)
  val totals = GpuBuffer(context, PAIR_BYTES, BufferUse.STORAGE)

  init {
    Arena.ofConfined().use { arena ->
      val gridData = arena.allocate(SurfaceNetsGridWriter.STRUCT_SIZE, Long.SIZE_BYTES.toLong())
      val writer = NativeWriter().begin(gridData)
      SurfaceNetsGridWriter.cellSize(writer, grid.cellSize)
      SurfaceNetsGridWriter.cellsX(writer, grid.cellsX.toUInt())
      SurfaceNetsGridWriter.cellsY(writer, grid.cellsY.toUInt())
      SurfaceNetsGridWriter.cellsZ(writer, grid.cellsZ.toUInt())
      SurfaceNetsGridWriter.points(writer, grid.pointCount.toUInt())
      SurfaceNetsGridWriter.blocks(writer, blocks.toUInt())
      this.grid.write(gridData)
      val nodeData = arena.allocate(nodes.bytes, Long.SIZE_BYTES.toLong())
      shader.writeNodes(nodeData)
      nodes.write(nodeData)
      val axisData = arena.allocate(axes.bytes, Long.SIZE_BYTES.toLong())
      var at = 0L
      for (axis in 0 until AXES) {
        for (index in 0 until cornersAlong(grid, axis)) {
          axisData.setAtIndex(ValueLayout.JAVA_FLOAT, at++, grid.coordinate(axis, index))
        }
      }
      axes.write(axisData)
    }
  }

  /** The buffers the emit pass reads, in its binding order before the mesh's own. */
  fun emitInputs(): List<GpuBuffer> = listOf(grid, nodes, samples, counts, offsets, blockSums, axes)

  override fun close() {
    listOf(grid, nodes, axes, samples, counts, offsets, blockSums, totals).forEach(GpuBuffer::close)
  }

  companion object {
    /**
     * Throws, naming what to mesh in parts, when a buffer for [grid] would outgrow [context]'s
     * device, before any is made.
     */
    fun checkFits(context: GpuContext, grid: SdfGrid, shader: SdfShader, blocks: Int) {
      val most = minOf(context.limits.maxBufferSize, context.limits.maxStorageBufferBindingSize)
      val largest =
        maxOf(grid.pointCount * PAIR_BYTES, shader.nodeBytes, blocks * PAIR_BYTES, axisBytes(grid))
      require(largest <= most) {
        "Meshing ${grid.pointCount} corners needs a buffer of $largest bytes, more than the " +
          "$most one may hold on this device; mesh the grid in parts"
      }
    }

    /** Bytes of every corner coordinate along x, then y, then z. */
    private fun axisBytes(grid: SdfGrid): Long =
      (grid.pointsX + grid.pointsY + grid.pointsZ).toLong() * Float.SIZE_BYTES

    private fun cornersAlong(grid: SdfGrid, axis: Int): Int =
      when (axis) {
        0 -> grid.pointsX
        1 -> grid.pointsY
        else -> grid.pointsZ
      }
  }
}
