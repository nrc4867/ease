package dev.chieppa.ease.sdf.gpu

import dev.chieppa.ease.gpu.CommandEncoder
import dev.chieppa.ease.gpu.GpuContext
import dev.chieppa.ease.gpu.compute.ComputeKernel
import dev.chieppa.ease.gpu.compute.GpuBuffer
import dev.chieppa.ease.gpu.mesh.ComputedMesh
import dev.chieppa.ease.gpu.mesh.VERTEX_BYTES
import dev.chieppa.ease.sdf.field.DistanceField
import dev.chieppa.ease.sdf.mesh.SdfGrid
import dev.chieppa.ease.sdf.mesh.SurfaceNets
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Surface nets on the GPU, in five compute passes over a grid: sample the field at every corner,
 * count each corner's vertex and quads, scan the counts in workgroups, scan the workgroups' totals,
 * then write every vertex and quad at its scanned offset. Corner coordinates come from a table
 * computed by [SdfGrid.coordinate], so corners sit on the same bits on both sides. The totals are
 * read back between the scans and the writes, so the mesh's buffers are made at their exact size.
 * Vertices and quads come in the order [SurfaceNets] gives them, at the same positions to within
 * rounding.
 */
public object GpuSurfaceNets {
  private const val WORKGROUP = SurfaceNetsShaders.WORKGROUP
  private const val INDICES_PER_QUAD = 6
  private const val INDEX_BYTES = 4L

  /** The most corners a grid meshed on the GPU may have: one invocation each, in one dispatch. */
  public const val MAX_POINTS: Long = WORKGROUP.toLong() * ComputeKernel.MAX_WORKGROUPS

  /**
   * The surface of [field] on [grid] as a mesh on [context]'s device, ready to draw. Compiles its
   * kernels, runs them and waits for them: load-time work. A grid with more than [MAX_POINTS]
   * corners, or whose buffers or mesh outgrow the device's limits, throws, naming what to mesh in
   * parts.
   */
  public fun mesh(context: GpuContext, field: DistanceField, grid: SdfGrid): ComputedMesh {
    require(grid.pointCount <= MAX_POINTS) {
      "${grid.pointCount} corners are more than the $MAX_POINTS one GPU mesh reaches; " +
        "mesh the grid in parts"
    }
    val blocks = (grid.pointCount + WORKGROUP - 1) / WORKGROUP
    val shader = SdfShader(field)
    SurfaceNetsScratch.checkFits(context, grid, shader, blocks)
    SurfaceNetsScratch(context, grid, shader, blocks).use { scratch ->
      count(context, shader, scratch, blocks)
      val counted = scratch.totals.read(Long.SIZE_BYTES.toLong())
      val vertices = intAt(counted, 0)
      val indices = intAt(counted, Int.SIZE_BYTES).toLong() * INDICES_PER_QUAD
      checkMeshFits(context, vertices.toLong(), indices)
      val mesh = ComputedMesh(context, vertices, indices.toInt())
      var written = false
      try {
        Passes(context).record {
          val buffers = scratch.emitInputs() + listOf(mesh.vertices, mesh.indices)
          run(SurfaceNetsShaders.emit(shader), "emit_main", blocks, buffers)
        }
        context.awaitIdle()
        context.checkErrors()
        written = true
        return mesh
      } finally {
        if (!written) mesh.close()
      }
    }
  }

  private fun count(
    context: GpuContext,
    shader: SdfShader,
    scratch: SurfaceNetsScratch,
    blocks: Int,
  ) {
    Passes(context).record {
      val sample = listOf(scratch.grid, scratch.nodes, scratch.axes, scratch.samples)
      run(SurfaceNetsShaders.sample(shader), "sample_main", blocks, sample)
      val classify = listOf(scratch.grid, scratch.samples, scratch.counts)
      run(SurfaceNetsShaders.classify(), "classify_main", blocks, classify)
      val scan = listOf(scratch.grid, scratch.counts, scratch.offsets, scratch.blockSums)
      run(SurfaceNetsShaders.scanBlocks(), "scan_blocks_main", blocks, scan)
      val totals = listOf(scratch.grid, scratch.blockSums, scratch.totals)
      run(SurfaceNetsShaders.scanTotals(), "scan_totals_main", 1, totals)
    }
  }

  private fun checkMeshFits(context: GpuContext, vertices: Long, indices: Long) {
    val most = minOf(context.limits.maxBufferSize, context.limits.maxStorageBufferBindingSize)
    val vertexBytes = vertices * VERTEX_BYTES
    val indexBytes = indices * INDEX_BYTES
    require(vertexBytes <= most && indexBytes <= most) {
      "The mesh needs $vertexBytes bytes of vertices and $indexBytes of indices, more than the " +
        "$most one buffer may hold on this device; mesh the grid in parts"
    }
  }

  private fun intAt(bytes: ByteArray, offset: Int): Int =
    ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getInt(offset)

  /**
   * Kernels recorded into one encoder on [context], each compiled as it is recorded, then
   * submitted; when recording fails the encoder is dropped unsubmitted.
   */
  private class Passes(private val context: GpuContext) {
    private val encoder = CommandEncoder.on(context)

    fun record(passes: Passes.() -> Unit) {
      var recorded = false
      try {
        passes()
        recorded = true
      } finally {
        if (!recorded) encoder.discard()
      }
      encoder.submit()
    }

    fun run(wgsl: String, entry: String, workgroups: Int, buffers: List<GpuBuffer>) {
      ComputeKernel(context, wgsl, entry).use { kernel ->
        kernel.bind(0, buffers).use { bindings ->
          kernel.dispatch(encoder, listOf(bindings), workgroups)
        }
      }
    }
  }
}
