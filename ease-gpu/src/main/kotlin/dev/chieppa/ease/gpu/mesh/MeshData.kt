package dev.chieppa.ease.gpu.mesh

import dev.chieppa.ease.memory.NativeWriter
import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

/** Bytes one vertex takes: position, normal and texture coordinates, all floats. */
public const val VERTEX_BYTES: Long = 32L

/** Offsets within a vertex. */
internal const val POSITION_OFFSET = 0L
internal const val NORMAL_OFFSET = 12L
internal const val UV_OFFSET = 24L

/** Bytes one index takes: indices are 32-bit. */
private const val INDEX_BYTES = 4L

/**
 * A triangle mesh in native memory, laid out the way the GPU draws it: vertices of position, normal
 * and texture coordinates, and 32-bit indices, three per counter-clockwise front face. It is the
 * dynamic mesh API: build it with [vertex] and [triangle], or [clear] and build it again, then
 * [commit], and the next frame that draws it uploads it. Room grows as it fills, starting at
 * [vertexCapacity] vertices and [indexCapacity] indices; a mesh rebuilt within its room allocates
 * nothing. Closing it frees the native memory.
 */
public class MeshData(vertexCapacity: Int = 64, indexCapacity: Int = 96) : AutoCloseable {
  init {
    require(vertexCapacity > 0 && indexCapacity > 0) {
      "Needs room for a vertex and an index, got $vertexCapacity and $indexCapacity"
    }
  }

  private val arena = Arena.ofShared()
  private val writer = NativeWriter()

  internal var vertices: MemorySegment =
    arena.allocate(vertexCapacity * VERTEX_BYTES, Long.SIZE_BYTES.toLong())
    private set

  internal var indices: MemorySegment = arena.allocate(indexCapacity * INDEX_BYTES, INDEX_BYTES)
    private set

  /** How many vertices the mesh holds. */
  public var vertexCount: Int = 0
    private set

  /** How many indices the mesh holds, three per triangle. */
  public var indexCount: Int = 0
    private set

  /** How many times the mesh was committed; a drawer uploads it again when this moves on. */
  public var version: Long = 0L
    private set

  /** The GPU's copy of this mesh, made by the first frame that draws it. */
  internal var uploaded: GpuMesh? = null

  /** Bytes the vertices take. */
  public val vertexBytes: Long
    get() = vertexCount * VERTEX_BYTES

  /** Bytes the indices take. */
  public val indexBytes: Long
    get() = indexCount * INDEX_BYTES

  /** Empties the mesh, keeping its room. */
  public fun clear() {
    vertexCount = 0
    indexCount = 0
  }

  /**
   * Adds a vertex at ([x], [y], [z]) with the unit normal ([nx], [ny], [nz]) and texture
   * coordinates ([u], [v]), and returns its index.
   */
  @Suppress("LongParameterList") // One vertex's eight floats, written without an object.
  public fun vertex(
    x: Float,
    y: Float,
    z: Float,
    nx: Float,
    ny: Float,
    nz: Float,
    u: Float,
    v: Float,
  ): Int {
    if ((vertexCount + 1) * VERTEX_BYTES > vertices.byteSize()) vertices = grow(vertices)
    writer.begin(vertices)
    writer.base = vertexCount * VERTEX_BYTES
    writer.vec3(POSITION_OFFSET, x, y, z)
    writer.vec3(NORMAL_OFFSET, nx, ny, nz)
    writer.vec2(UV_OFFSET, u, v)
    return vertexCount++
  }

  /** Adds the triangle of vertices [a], [b] and [c], counter-clockwise seen from its front. */
  public fun triangle(a: Int, b: Int, c: Int) {
    require(a in 0 until vertexCount && b in 0 until vertexCount && c in 0 until vertexCount) {
      "Triangle ($a, $b, $c) names a vertex the mesh's $vertexCount do not include"
    }
    if ((indexCount + TRIANGLE) * INDEX_BYTES > indices.byteSize()) indices = grow(indices)
    indices.setAtIndex(ValueLayout.JAVA_INT, indexCount.toLong(), a)
    indices.setAtIndex(ValueLayout.JAVA_INT, indexCount + 1L, b)
    indices.setAtIndex(ValueLayout.JAVA_INT, indexCount + 2L, c)
    indexCount += TRIANGLE
  }

  /** Marks the mesh as built, so the next frame drawing it uploads it. */
  public fun commit() {
    version++
  }

  override fun close() {
    uploaded?.close()
    uploaded = null
    arena.close()
  }

  private fun grow(old: MemorySegment): MemorySegment {
    val grown = arena.allocate(old.byteSize() * 2, Long.SIZE_BYTES.toLong())
    MemorySegment.copy(old, 0, grown, 0, old.byteSize())
    return grown
  }

  private companion object {
    const val TRIANGLE = 3
  }
}
