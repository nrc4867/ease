package dev.chieppa.ease.gpu.mesh

import dev.chieppa.ease.gpu.GpuContext
import dev.chieppa.ease.gpu.compute.BufferUse
import dev.chieppa.ease.gpu.compute.GpuBuffer
import java.lang.foreign.MemorySegment

/** Bytes one index takes. */
private const val INDEX_BYTES = 4L

/**
 * A triangle mesh that lives only on one device, written there by compute shaders rather than
 * uploaded: [vertexCount] vertices in [MeshData]'s layout and [indexCount] 32-bit indices, three
 * per counter-clockwise front face. A lit pass draws it like a [MeshData]. Shaders write its
 * vertices with [WRITE_VERTEX_WGSL] and its indices into an `array<u32>`. Closing it frees its
 * buffers.
 */
public class ComputedMesh(
  context: GpuContext,
  public val vertexCount: Int,
  public val indexCount: Int,
) : AutoCloseable {
  init {
    require(vertexCount >= 0 && indexCount >= 0 && indexCount % TRIANGLE == 0) {
      "A mesh needs whole triangles; got $vertexCount vertices and $indexCount indices"
    }
  }

  /** The vertices, bound as storage where a shader writes them. */
  public val vertices: GpuBuffer =
    GpuBuffer(context, vertexCount * VERTEX_BYTES, BufferUse.VERTICES)

  /** The indices, bound as storage where a shader writes them. */
  public val indices: GpuBuffer = GpuBuffer(context, indexCount * INDEX_BYTES, BufferUse.INDICES)

  /** The buffers as a pass draws them. */
  internal val buffers: MeshBuffers =
    object : MeshBuffers {
      override val vertexBuffer: MemorySegment = vertices.handle
      override val indexBuffer: MemorySegment = indices.handle
      override val vertexBytes: Long = vertices.bytes
      override val indexBytes: Long = indices.bytes
      override val indexCount: Int = this@ComputedMesh.indexCount
    }

  override fun close() {
    vertices.close()
    indices.close()
  }

  public companion object {
    private const val TRIANGLE = 3

    /**
     * WGSL defining `mesh_write_vertex(index: u32, position: vec3f, normal: vec3f)`, which writes
     * one vertex in this mesh's layout, with zero texture coordinates, into `mesh_vertices`: a
     * `var<storage, read_write> mesh_vertices: array<f32>` the shader binds to [vertices].
     */
    public val WRITE_VERTEX_WGSL: String =
      """
      fn mesh_write_vertex(index: u32, position: vec3f, normal: vec3f) {
        let at = index * ${VERTEX_BYTES / Float.SIZE_BYTES}u;
        mesh_vertices[at + ${POSITION_OFFSET / Float.SIZE_BYTES}u] = position.x;
        mesh_vertices[at + ${POSITION_OFFSET / Float.SIZE_BYTES + 1}u] = position.y;
        mesh_vertices[at + ${POSITION_OFFSET / Float.SIZE_BYTES + 2}u] = position.z;
        mesh_vertices[at + ${NORMAL_OFFSET / Float.SIZE_BYTES}u] = normal.x;
        mesh_vertices[at + ${NORMAL_OFFSET / Float.SIZE_BYTES + 1}u] = normal.y;
        mesh_vertices[at + ${NORMAL_OFFSET / Float.SIZE_BYTES + 2}u] = normal.z;
        mesh_vertices[at + ${UV_OFFSET / Float.SIZE_BYTES}u] = 0.0;
        mesh_vertices[at + ${UV_OFFSET / Float.SIZE_BYTES + 1}u] = 0.0;
      }
      """
        .trimIndent()

    /** Floats one vertex takes, for reading [vertices] back. */
    public const val VERTEX_FLOATS: Int = (VERTEX_BYTES / Float.SIZE_BYTES).toInt()
  }
}
