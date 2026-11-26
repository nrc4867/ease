package dev.chieppa.ease.gpu.compute

import dev.chieppa.ease.gpu.CommandEncoder
import dev.chieppa.ease.gpu.GpuContext
import dev.chieppa.ease.gpu.ffi.Flags
import dev.chieppa.ease.gpu.ffi.createBuffer
import dev.chieppa.ease.gpu.ffi.readMapped
import dev.chieppa.webgpu.ffi.wgpu_h
import java.lang.foreign.MemorySegment

/** WebGPU sizes and offsets of buffer copies and writes are multiples of this many bytes. */
private const val COPY_ALIGNMENT = 4L

/**
 * A buffer of at least [bytes] on [context]'s device, for [use]. Its size is rounded up to a
 * multiple of 4 bytes and is never zero, as WebGPU asks; a size past the device's limits throws.
 * Making, writing and reading one are load-time work: [read] allocates and blocks.
 */
public class GpuBuffer(
  private val context: GpuContext,
  bytes: Long,
  public val use: BufferUse,
) : AutoCloseable {
  init {
    require(bytes >= 0) { "A buffer cannot hold $bytes bytes" }
  }

  /** The buffer's size in bytes. */
  public val bytes: Long = maxOf(COPY_ALIGNMENT, aligned(bytes))

  init {
    val limits = context.limits
    require(this.bytes <= limits.maxBufferSize) {
      "A buffer of ${this.bytes} bytes is larger than the device's ${limits.maxBufferSize}"
    }
    require(use == BufferUse.UNIFORM || this.bytes <= limits.maxStorageBufferBindingSize) {
      "A $use buffer of ${this.bytes} bytes is larger than one storage binding covers, " +
        "${limits.maxStorageBufferBindingSize}"
    }
  }

  internal val handle: MemorySegment =
    createBuffer(
      context.device,
      this.bytes,
      use.flags or Flags.BUFFER_COPY_DST or Flags.BUFFER_COPY_SRC,
    )

  /** Queues a write of the first [count] bytes of [data] into this buffer from its start. */
  public fun write(data: MemorySegment, count: Long = data.byteSize()) {
    require(count in 0..bytes) { "$count bytes do not fit a buffer of $bytes" }
    require(count <= data.byteSize()) { "$count bytes are more than the ${data.byteSize()} given" }
    require(count % COPY_ALIGNMENT == 0L) { "Writes are multiples of 4 bytes, not $count" }
    if (count > 0) wgpu_h.wgpuQueueWriteBuffer(context.queue, handle, 0, data, count)
  }

  /**
   * The first [count] bytes of this buffer once the GPU has finished everything submitted before:
   * copied into a buffer the CPU can map, read and returned. Blocks until then.
   */
  public fun read(count: Long = bytes): ByteArray {
    require(count in 0..bytes) { "$count bytes are more than a buffer of $bytes holds" }
    if (count == 0L) return ByteArray(0)
    val copyBytes = aligned(count)
    val staging =
      createBuffer(context.device, copyBytes, Flags.BUFFER_COPY_DST or Flags.BUFFER_MAP_READ)
    try {
      val encoder = CommandEncoder.on(context)
      wgpu_h.wgpuCommandEncoderCopyBufferToBuffer(encoder.handle, handle, 0, staging, 0, copyBytes)
      encoder.submit()
      val read = readMapped(context, staging, copyBytes)
      context.checkErrors()
      return if (copyBytes == count) read else read.copyOf(count.toInt())
    } finally {
      wgpu_h.wgpuBufferRelease(staging)
    }
  }

  override fun close() {
    wgpu_h.wgpuBufferRelease(handle)
  }

  private companion object {
    fun aligned(bytes: Long): Long = (bytes + COPY_ALIGNMENT - 1) / COPY_ALIGNMENT * COPY_ALIGNMENT
  }
}
