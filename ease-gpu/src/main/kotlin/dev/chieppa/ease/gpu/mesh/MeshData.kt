package dev.chieppa.ease.gpu.mesh

import dev.chieppa.ease.memory.MappedRegion
import dev.chieppa.ease.memory.NativeWriter
import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

/**
 * Bytes one vertex takes, all floats: position (location 0), normal (1), texture coordinates (2),
 * then the tangent (3) as x, y, z and w, w the handedness (1 or -1) of the bitangent, which is
 * cross(normal, tangent) times w.
 */
public const val VERTEX_BYTES: Long = 48L

/** Byte offset of a vertex's position, three floats. */
public const val POSITION_OFFSET: Long = 0L

/** Byte offset of a vertex's unit normal, three floats. */
public const val NORMAL_OFFSET: Long = 12L

/** Byte offset of a vertex's texture coordinates, two floats. */
public const val UV_OFFSET: Long = 24L

/** Byte offset of a vertex's tangent and handedness, four floats. */
public const val TANGENT_OFFSET: Long = 32L

/** Bytes one index takes: indices are 32-bit. */
public const val INDEX_BYTES: Long = 4L

/**
 * A triangle mesh in native memory, laid out the way the GPU draws it: vertices of position,
 * normal, texture coordinates and tangent ([VERTEX_BYTES] says where each is), and 32-bit indices,
 * three per counter-clockwise front face. It is the dynamic mesh API: build it with [vertex] and
 * [triangle], or [clear] and build it again, then [commit], and the next frame that draws it
 * uploads it. Room grows as it fills, starting at [vertexCapacity] vertices and [indexCapacity]
 * indices; a mesh rebuilt within its room allocates nothing. Closing it frees the native memory.
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
   * coordinates ([u], [v]), and returns its index. Its tangent is +x with handedness 1; [tangent]
   * sets another.
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
    vertices = room(vertices, (vertexCount + 1) * VERTEX_BYTES)
    writer.begin(vertices)
    writer.base = vertexCount * VERTEX_BYTES
    writer.vec3(POSITION_OFFSET, x, y, z)
    writer.vec3(NORMAL_OFFSET, nx, ny, nz)
    writer.vec2(UV_OFFSET, u, v)
    writer.vec4(TANGENT_OFFSET, 1f, 0f, 0f, 1f)
    return vertexCount++
  }

  /** Sets vertex [index]'s unit tangent ([x], [y], [z]) and handedness [w], 1 or -1. */
  public fun tangent(index: Int, x: Float, y: Float, z: Float, w: Float) {
    require(index in 0 until vertexCount) { "No vertex $index among $vertexCount" }
    writer.begin(vertices)
    writer.base = index * VERTEX_BYTES
    writer.vec4(TANGENT_OFFSET, x, y, z, w)
  }

  /**
   * Replaces the mesh with [vertexCount] vertices copied from [vertexBytes] (laid out as
   * [VERTEX_BYTES] says) and [indexCount] indices from [indexBytes], each read from its offset 0,
   * then commits it. Indices must name the new vertices; the copy goes straight between native
   * memory.
   */
  public fun replace(
    vertexBytes: MappedRegion,
    vertexCount: Int,
    indexBytes: MappedRegion,
    indexCount: Int,
  ) {
    require(indexCount % TRIANGLE == 0) { "$indexCount indices are not whole triangles" }
    vertices = room(vertices, vertexCount * VERTEX_BYTES)
    indices = room(indices, indexCount * INDEX_BYTES)
    vertexBytes.copyTo(0, vertices, 0, vertexCount * VERTEX_BYTES)
    indexBytes.copyTo(0, indices, 0, indexCount * INDEX_BYTES)
    this.vertexCount = vertexCount
    this.indexCount = indexCount
    commit()
  }

  /** Adds the triangle of vertices [a], [b] and [c], counter-clockwise seen from its front. */
  public fun triangle(a: Int, b: Int, c: Int) {
    require(a in 0 until vertexCount && b in 0 until vertexCount && c in 0 until vertexCount) {
      "Triangle ($a, $b, $c) names a vertex the mesh's $vertexCount do not include"
    }
    indices = room(indices, (indexCount + TRIANGLE) * INDEX_BYTES)
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

  /** [old], or when it is smaller than [needed] bytes, a copy at least twice as large. */
  private fun room(old: MemorySegment, needed: Long): MemorySegment {
    if (needed <= old.byteSize()) return old
    val grown = arena.allocate(maxOf(old.byteSize() * 2, needed), Long.SIZE_BYTES.toLong())
    MemorySegment.copy(old, 0, grown, 0, old.byteSize())
    return grown
  }

  private companion object {
    const val TRIANGLE = 3
  }
}
