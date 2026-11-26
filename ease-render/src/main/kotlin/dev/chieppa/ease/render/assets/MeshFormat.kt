package dev.chieppa.ease.render.assets

import dev.chieppa.ease.gpu.mesh.INDEX_BYTES
import dev.chieppa.ease.gpu.mesh.MeshData
import dev.chieppa.ease.gpu.mesh.VERTEX_BYTES
import dev.chieppa.ease.memory.MappedRegion
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** The streams of one mesh before it is cooked: [MeshStreams.vertexCount] vertices and indices. */
@Suppress("LongParameterList") // One array a vertex stream, the optional ones null.
public class MeshStreams(
  public val positions: FloatArray,
  public val normals: FloatArray,
  public val uv0: FloatArray,
  public val tangents: FloatArray,
  public val indices: IntArray,
  public val joints: IntArray? = null,
  public val weights: FloatArray? = null,
  public val uv1: FloatArray? = null,
  public val colors: FloatArray? = null,
) {
  /** How many vertices the streams hold. */
  public val vertexCount: Int = positions.size / MeshFormat.POSITION_FLOATS
}

/**
 * The cooked mesh file (`.mesh`), little-endian: the magic `EMSH`, the format version, vertex and
 * index counts, a flags word (1 joints and weights, 2 second texture coordinates, 4 colors), the
 * bounds' minimum and maximum, then the vertices exactly as [MeshData] lays them out
 * ([VERTEX_BYTES] each), the 32-bit indices, and the optional streams in flag order: joints as four
 * 16-bit indices and weights as four floats a vertex, two floats of second coordinates, four floats
 * of color. Loading copies the vertices and indices straight from the mapped file into the mesh.
 */
public object MeshFormat {
  /** The file's first four bytes, `EMSH`. */
  public const val MAGIC: Int = 0x48534D45

  /** The format version this build writes and reads. */
  public const val VERSION: Int = 1

  /** Floats of one position. */
  public const val POSITION_FLOATS: Int = 3

  private const val JOINTS = 1
  private const val UV1 = 2
  private const val COLORS = 4
  private const val HEADER_INTS = 5
  private const val BOUNDS_FLOATS = 6
  private const val HEADER_BYTES = (HEADER_INTS + BOUNDS_FLOATS) * Int.SIZE_BYTES
  private const val NORMAL_FLOATS = 3
  private const val UV_FLOATS = 2
  private const val QUAD = 4
  private const val SHORT_MASK = 0xFFFF
  private const val FLAGS_AT = 16L
  private const val BOUNDS_AT = 20L

  /** The cooked bytes of [streams]. */
  public fun write(streams: MeshStreams): ByteArray {
    val n = streams.vertexCount
    var flags = 0
    if (streams.joints != null && streams.weights != null) flags = flags or JOINTS
    if (streams.uv1 != null) flags = flags or UV1
    if (streams.colors != null) flags = flags or COLORS
    val size =
      HEADER_BYTES + n * VERTEX_BYTES + streams.indices.size * INDEX_BYTES + extraBytes(flags, n)
    val out = ByteBuffer.allocate(size.toInt()).order(ByteOrder.LITTLE_ENDIAN)
    out.putInt(MAGIC).putInt(VERSION)
    out.putInt(n).putInt(streams.indices.size).putInt(flags)
    bounds(streams.positions, n).forEach { out.putFloat(it) }
    for (v in 0 until n) {
      put(out, streams.positions, v * POSITION_FLOATS, POSITION_FLOATS)
      put(out, streams.normals, v * NORMAL_FLOATS, NORMAL_FLOATS)
      put(out, streams.uv0, v * UV_FLOATS, UV_FLOATS)
      put(out, streams.tangents, v * QUAD, QUAD)
    }
    streams.indices.forEach { out.putInt(it) }
    if (flags and JOINTS != 0) {
      checkNotNull(streams.joints).forEach { out.putShort(it.toShort()) }
      checkNotNull(streams.weights).forEach { out.putFloat(it) }
    }
    streams.uv1?.forEach { out.putFloat(it) }
    streams.colors?.forEach { out.putFloat(it) }
    return out.array()
  }

  /** The mesh [region] holds, its vertices and indices copied into a new [MeshData]. */
  public fun read(region: MappedRegion): MeshAsset {
    require(region.int32(0) == MAGIC) { "not a cooked mesh" }
    val version = region.int32(Int.SIZE_BYTES.toLong())
    require(version == VERSION) { "cooked mesh version $version, this build reads $VERSION" }
    val n = region.int32(2L * Int.SIZE_BYTES)
    val indexCount = region.int32(3L * Int.SIZE_BYTES)
    val flags = region.int32(FLAGS_AT)
    val bounds = FloatArray(BOUNDS_FLOATS) { region.float32(BOUNDS_AT + it * Float.SIZE_BYTES) }
    var at = HEADER_BYTES.toLong()
    val vertices = region.region(at, n * VERTEX_BYTES)
    at += n * VERTEX_BYTES
    val indices = region.region(at, indexCount * INDEX_BYTES)
    at += indexCount * INDEX_BYTES
    val mesh = MeshData(maxOf(n, 1), maxOf(indexCount, 1))
    mesh.replace(vertices, n, indices, indexCount)
    var joints: IntArray? = null
    var weights: FloatArray? = null
    if (flags and JOINTS != 0) {
      joints = IntArray(n * QUAD) { region.uint16(at + it * Short.SIZE_BYTES) and SHORT_MASK }
      at += n * QUAD * Short.SIZE_BYTES
      weights = floats(region, at, n * QUAD)
      at += n * QUAD * Float.SIZE_BYTES
    }
    val uv1 = if (flags and UV1 != 0) floats(region, at, n * UV_FLOATS) else null
    if (uv1 != null) at += uv1.size * Float.SIZE_BYTES
    val colors = if (flags and COLORS != 0) floats(region, at, n * QUAD) else null
    return MeshAsset(
      mesh,
      bounds.copyOf(POSITION_FLOATS),
      bounds.copyOfRange(POSITION_FLOATS, BOUNDS_FLOATS),
      joints,
      weights,
      uv1,
      colors,
    )
  }

  private fun floats(region: MappedRegion, at: Long, count: Int): FloatArray =
    FloatArray(count) { region.float32(at + it * Float.SIZE_BYTES) }

  private fun extraBytes(flags: Int, n: Int): Long {
    var bytes = 0L
    if (flags and JOINTS != 0) bytes += n * QUAD * (Short.SIZE_BYTES + Float.SIZE_BYTES).toLong()
    if (flags and UV1 != 0) bytes += n * UV_FLOATS * Float.SIZE_BYTES.toLong()
    if (flags and COLORS != 0) bytes += n * QUAD * Float.SIZE_BYTES.toLong()
    return bytes
  }

  private fun put(out: ByteBuffer, values: FloatArray, at: Int, count: Int) {
    for (i in 0 until count) out.putFloat(values[at + i])
  }

  private fun bounds(positions: FloatArray, n: Int): FloatArray {
    if (n == 0) return FloatArray(BOUNDS_FLOATS)
    val out = FloatArray(BOUNDS_FLOATS)
    for (axis in 0 until POSITION_FLOATS) {
      out[axis] = Float.POSITIVE_INFINITY
      out[axis + POSITION_FLOATS] = Float.NEGATIVE_INFINITY
    }
    for (v in 0 until n) {
      for (axis in 0 until POSITION_FLOATS) {
        val value = positions[v * POSITION_FLOATS + axis]
        out[axis] = minOf(out[axis], value)
        out[axis + POSITION_FLOATS] = maxOf(out[axis + POSITION_FLOATS], value)
      }
    }
    return out
  }
}
