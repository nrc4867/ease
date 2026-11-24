package dev.chieppa.ease.gpu

import dev.chieppa.webgpu.ffi.wgpu_h
import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment

/**
 * Records GPU commands for one submission. Passes record into it; [submit] sends them to the queue.
 * A host that already records commands hands its own encoder over with [borrowed].
 */
public class CommandEncoder
internal constructor(internal val handle: MemorySegment, private val context: GpuContext?) {
  /** Finishes recording and submits the commands to the context's queue. */
  public fun submit() {
    val owner = checkNotNull(context) { "A borrowed encoder is submitted by its owner" }
    val commands = wgpu_h.wgpuCommandEncoderFinish(handle, MemorySegment.NULL)
    Arena.ofConfined().use { arena ->
      val list = arena.allocate(wgpu_h.C_POINTER)
      list.set(wgpu_h.C_POINTER, 0, commands)
      wgpu_h.wgpuQueueSubmit(owner.queue, 1, list)
    }
    wgpu_h.wgpuCommandBufferRelease(commands)
    wgpu_h.wgpuCommandEncoderRelease(handle)
  }

  public companion object {
    /** A new encoder on [context]'s device. */
    public fun on(context: GpuContext): CommandEncoder =
      CommandEncoder(
        wgpu_h.wgpuDeviceCreateCommandEncoder(context.device, MemorySegment.NULL),
        context,
      )

    /**
     * An encoder someone else made and submits, such as an editor viewport's, given as its wgpu
     * handle address.
     */
    public fun borrowed(handle: Long): CommandEncoder =
      CommandEncoder(MemorySegment.ofAddress(handle), null)
  }
}
