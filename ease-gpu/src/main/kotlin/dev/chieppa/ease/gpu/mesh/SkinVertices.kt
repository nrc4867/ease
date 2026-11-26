package dev.chieppa.ease.gpu.mesh

import dev.chieppa.ease.gpu.GpuContext
import dev.chieppa.ease.gpu.ffi.Flags
import dev.chieppa.ease.gpu.ffi.createBuffer
import dev.chieppa.webgpu.ffi.wgpu_h
import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

/** Bytes one skinned vertex's joints and weights take: four 16-bit joints, four float weights. */
public const val SKIN_VERTEX_BYTES: Long = 24L

/** Byte offset of a skinned vertex's four 16-bit joint indices. */
public const val JOINTS_OFFSET: Long = 0L

/** Byte offset of a skinned vertex's four float weights. */
public const val WEIGHTS_OFFSET: Long = 8L

private const val INFLUENCES = 4

/**
 * The second vertex stream of a skinned mesh, in native memory beside its [MeshData]: for each of
 * [vertexCount] vertices, the four joints it follows (indices into its skin's joint matrices) and
 * how much each pulls ([SKIN_VERTEX_BYTES] each). A lit pass draws a mesh with it bent by a joint
 * palette. It is made once from [joints] and [weights] (four a vertex) and uploaded by the first
 * frame that draws it. Closing it frees the native memory and the GPU copy.
 */
public class SkinVertices(joints: IntArray, weights: FloatArray) : AutoCloseable {
  /** How many vertices it holds. */
  public val vertexCount: Int = joints.size / INFLUENCES

  init {
    require(joints.size == weights.size && joints.size % INFLUENCES == 0) {
      "Needs four joints and four weights a vertex, got ${joints.size} and ${weights.size}"
    }
    require(joints.all { it in 0..MAX_JOINT }) { "Joint indices run from 0 to $MAX_JOINT" }
  }

  private val arena = Arena.ofShared()

  internal val bytes: MemorySegment =
    arena.allocate(maxOf(vertexCount, 1) * SKIN_VERTEX_BYTES, Long.SIZE_BYTES.toLong()).also {
      for (vertex in 0 until vertexCount) {
        val base = vertex * SKIN_VERTEX_BYTES
        for (slot in 0 until INFLUENCES) {
          val at = vertex * INFLUENCES + slot
          it.set(SHORT, base + JOINTS_OFFSET + slot * Short.SIZE_BYTES, joints[at].toShort())
          it.set(FLOAT, base + WEIGHTS_OFFSET + slot * Float.SIZE_BYTES, weights[at])
        }
      }
    }

  /** Bytes the stream takes. */
  public val byteSize: Long
    get() = vertexCount * SKIN_VERTEX_BYTES

  private var uploaded: MemorySegment = MemorySegment.NULL
  private var uploadedTo: GpuContext? = null

  /** The stream's buffer on [context]'s device, uploaded the first time it is asked for there. */
  internal fun buffer(context: GpuContext): MemorySegment {
    if (uploadedTo === context) return uploaded
    release()
    uploaded = createBuffer(context.device, maxOf(byteSize, SKIN_VERTEX_BYTES), USAGE)
    if (byteSize > 0) wgpu_h.wgpuQueueWriteBuffer(context.queue, uploaded, 0, bytes, byteSize)
    uploadedTo = context
    return uploaded
  }

  override fun close() {
    release()
    arena.close()
  }

  private fun release() {
    if (uploaded != MemorySegment.NULL) wgpu_h.wgpuBufferRelease(uploaded)
    uploaded = MemorySegment.NULL
    uploadedTo = null
  }

  private companion object {
    const val MAX_JOINT = 0xFFFF
    const val USAGE = Flags.BUFFER_VERTEX or Flags.BUFFER_COPY_DST
    val SHORT: ValueLayout.OfShort = ValueLayout.JAVA_SHORT_UNALIGNED
    val FLOAT: ValueLayout.OfFloat = ValueLayout.JAVA_FLOAT_UNALIGNED
  }
}
