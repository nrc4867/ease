package dev.chieppa.ease.sdf.mesh

import dev.chieppa.ease.gpu.CommandEncoder
import dev.chieppa.ease.gpu.GpuContext
import dev.chieppa.ease.gpu.compute.BufferUse
import dev.chieppa.ease.gpu.compute.ComputeKernel
import dev.chieppa.ease.gpu.compute.GpuBuffer
import dev.chieppa.ease.gpu.mesh.ComputedMesh
import dev.chieppa.ease.memory.NativeWriter
import dev.chieppa.ease.sdf.field.DistanceField
import java.lang.foreign.Arena
import java.lang.foreign.ValueLayout
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Surface nets on the GPU, in five compute passes over a grid: sample the field at every corner,
 * count each corner's vertex and quads, scan the counts in workgroups of 256, scan the workgroups'
 * totals, then write every vertex and quad at its scanned offset. Corner coordinates come from a
 * table computed as [SurfaceNets] computes them, so corners sit on the same bits on both sides. The
 * totals are read back between the scans and the writes, so the mesh's buffers are made at their
 * exact size. Vertices and quads come in the order [SurfaceNets] gives them, at the same positions
 * to within rounding.
 */
public object GpuSurfaceNets {
  private const val WORKGROUP = 256
  private const val PAIR_BYTES = 8L
  private const val INDICES_PER_QUAD = 6

  /**
   * The surface of [field] on [grid] as a mesh on [context]'s device, ready to draw. Compiles its
   * kernels, runs them and waits for them: load-time work.
   */
  public fun mesh(context: GpuContext, field: DistanceField, grid: SdfGrid): ComputedMesh {
    val blocks = (grid.pointCount + WORKGROUP - 1) / WORKGROUP
    val scratch = Scratch(context, field, grid, blocks)
    try {
      val counting = Passes(context)
      val sample = listOf(scratch.grid, scratch.nodes, scratch.axes, scratch.samples)
      counting.run(SurfaceNetsShaders.sample(field), "sample_main", blocks, sample)
      val classify = listOf(scratch.grid, scratch.samples, scratch.counts)
      counting.run(SurfaceNetsShaders.classify(), "classify_main", blocks, classify)
      val scan = listOf(scratch.grid, scratch.counts, scratch.offsets, scratch.blockSums)
      counting.run(SurfaceNetsShaders.scanBlocks(), "scan_blocks_main", blocks, scan)
      val totals = listOf(scratch.grid, scratch.blockSums, scratch.totals)
      counting.run(SurfaceNetsShaders.scanTotals(), "scan_totals_main", 1, totals)
      counting.encoder.submit()
      val counted = scratch.totals.read(PAIR_BYTES)
      val vertices = intAt(counted, 0)
      val quads = intAt(counted, Int.SIZE_BYTES)
      val mesh = ComputedMesh(context, vertices, quads * INDICES_PER_QUAD)
      val writing = Passes(context)
      val emit =
        listOf(
          scratch.grid,
          scratch.nodes,
          scratch.samples,
          scratch.counts,
          scratch.offsets,
          scratch.blockSums,
          scratch.axes,
          mesh.vertices,
          mesh.indices,
        )
      writing.run(SurfaceNetsShaders.emit(field), "emit_main", blocks, emit)
      writing.encoder.submit()
      context.awaitIdle()
      context.checkErrors()
      return mesh
    } finally {
      scratch.close()
    }
  }

  private fun intAt(bytes: ByteArray, offset: Int): Int =
    ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getInt(offset)

  /** The buffers the passes share, freed once the mesh is written. */
  private class Scratch(context: GpuContext, field: DistanceField, grid: SdfGrid, blocks: Int) :
    AutoCloseable {
    private val points = grid.pointCount.toLong()
    val grid = GpuBuffer(context, SurfaceNetsGridWriter.STRUCT_SIZE, BufferUse.UNIFORM)
    val nodes = GpuBuffer(context, field.nodeBytes, BufferUse.STORAGE)
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
        field.writeNodes(nodeData)
        nodes.write(nodeData)
        val axisData = arena.allocate(axes.bytes, Long.SIZE_BYTES.toLong())
        var at = 0L
        for ((axis, count) in listOf(grid.pointsX, grid.pointsY, grid.pointsZ).withIndex()) {
          for (index in 0 until count) {
            axisData.setAtIndex(
              ValueLayout.JAVA_FLOAT,
              at++,
              SurfaceNets.coordinate(grid, axis, index),
            )
          }
        }
        axes.write(axisData)
      }
    }

    override fun close() {
      listOf(grid, nodes, axes, samples, counts, offsets, blockSums, totals)
        .forEach(GpuBuffer::close)
    }
  }

  /** Bytes of every corner coordinate along x, then y, then z. */
  private fun axisBytes(grid: SdfGrid): Long =
    (grid.pointsX + grid.pointsY + grid.pointsZ).toLong() * Float.SIZE_BYTES

  /** Kernels recorded into one encoder on [context], each compiled as it is recorded. */
  private class Passes(private val context: GpuContext) {
    val encoder: CommandEncoder = CommandEncoder.on(context)

    fun run(wgsl: String, entry: String, workgroups: Int, buffers: List<GpuBuffer>) {
      ComputeKernel(context, wgsl, entry).use { kernel ->
        kernel.bind(0, buffers).use { bindings ->
          kernel.dispatch(encoder, listOf(bindings), workgroups)
        }
      }
    }
  }
}
