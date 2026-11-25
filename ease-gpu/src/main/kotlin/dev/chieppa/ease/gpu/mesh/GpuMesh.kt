package dev.chieppa.ease.gpu.mesh

import dev.chieppa.ease.gpu.GpuContext
import dev.chieppa.ease.gpu.ffi.Flags
import dev.chieppa.ease.gpu.ffi.createBuffer
import dev.chieppa.webgpu.ffi.wgpu_h
import java.lang.foreign.MemorySegment

/**
 * A [MeshData]'s vertex and index buffers on one device, uploaded straight from the mesh's native
 * memory. Buffers are made again only when the mesh outgrows them.
 */
internal class GpuMesh(val context: GpuContext) : MeshBuffers, AutoCloseable {
  override var vertexBuffer: MemorySegment = MemorySegment.NULL
    private set

  override var indexBuffer: MemorySegment = MemorySegment.NULL
    private set

  private var vertexRoom = 0L
  private var indexRoom = 0L
  private var version = -1L

  /** Bytes of vertices and indices uploaded. */
  override var vertexBytes = 0L
    private set

  override var indexBytes = 0L
    private set

  /** Indices uploaded, three per triangle. */
  override var indexCount = 0
    private set

  /** Uploads [mesh] when this holds an older version of it. */
  fun sync(mesh: MeshData) {
    if (mesh.version == version) return
    if (mesh.vertexBytes > vertexRoom) {
      release(vertexBuffer)
      vertexRoom = mesh.vertices.byteSize()
      vertexBuffer = createBuffer(context.device, vertexRoom, VERTEX_USAGE)
    }
    if (mesh.indexBytes > indexRoom) {
      release(indexBuffer)
      indexRoom = mesh.indices.byteSize()
      indexBuffer = createBuffer(context.device, indexRoom, INDEX_USAGE)
    }
    if (mesh.vertexBytes > 0) {
      wgpu_h.wgpuQueueWriteBuffer(context.queue, vertexBuffer, 0, mesh.vertices, mesh.vertexBytes)
    }
    if (mesh.indexBytes > 0) {
      wgpu_h.wgpuQueueWriteBuffer(context.queue, indexBuffer, 0, mesh.indices, mesh.indexBytes)
    }
    vertexBytes = mesh.vertexBytes
    indexBytes = mesh.indexBytes
    indexCount = mesh.indexCount
    version = mesh.version
  }

  override fun close() {
    release(vertexBuffer)
    release(indexBuffer)
    vertexBuffer = MemorySegment.NULL
    indexBuffer = MemorySegment.NULL
  }

  private fun release(buffer: MemorySegment) {
    if (buffer != MemorySegment.NULL) wgpu_h.wgpuBufferRelease(buffer)
  }

  companion object {
    private const val VERTEX_USAGE = Flags.BUFFER_VERTEX or Flags.BUFFER_COPY_DST
    private const val INDEX_USAGE = Flags.BUFFER_INDEX or Flags.BUFFER_COPY_DST

    /** [mesh] on [context]'s device, uploaded when it changed since it was last drawn there. */
    fun of(context: GpuContext, mesh: MeshData): GpuMesh {
      val existing = mesh.uploaded
      val gpu =
        if (existing != null && existing.context === context) {
          existing
        } else {
          existing?.close()
          GpuMesh(context).also { mesh.uploaded = it }
        }
      gpu.sync(mesh)
      return gpu
    }
  }
}
