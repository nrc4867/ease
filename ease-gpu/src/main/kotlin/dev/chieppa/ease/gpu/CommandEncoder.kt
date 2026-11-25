package dev.chieppa.ease.gpu

import dev.chieppa.ease.gpu.ffi.FrameCalls
import dev.chieppa.webgpu.ffi.wgpu_h
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

private const val SUBMIT_SITE = "CommandEncoder.submit"

/**
 * Records GPU commands for one submission. Passes record into it; [submit] sends them to the queue.
 * A host that already records commands hands its own encoder over with [borrowed]; a frame loop
 * takes [GpuContext.frameEncoder], which makes no object per frame.
 */
public class CommandEncoder internal constructor(address: Long, private val context: GpuContext?) {
  /** The wgpu encoder's address. */
  internal var address: Long = address
    private set

  /** The wgpu encoder, for calls made once rather than every frame. */
  internal val handle: MemorySegment
    get() = MemorySegment.ofAddress(address)

  /** Points this object at a new encoder on its context's device, for the next frame. */
  internal fun restart(): CommandEncoder {
    val owner = checkNotNull(context) { "A borrowed encoder cannot restart" }
    address = FrameCalls.createCommandEncoder(owner.device)
    return this
  }

  /** Finishes recording and submits the commands to the context's queue. */
  public fun submit() {
    val owner = checkNotNull(context) { "A borrowed encoder is submitted by its owner" }
    val commands = FrameCalls.finish(address)
    val list = owner.frames.take(wgpu_h.C_POINTER.byteSize(), SUBMIT_SITE).segment
    list.set(ValueLayout.JAVA_LONG, 0, commands)
    wgpu_h.wgpuQueueSubmit(owner.queue, 1, list)
    FrameCalls.releaseCommands(commands)
    FrameCalls.releaseEncoder(address)
  }

  public companion object {
    /** A new encoder on [context]'s device. */
    public fun on(context: GpuContext): CommandEncoder = CommandEncoder(0L, context).restart()

    /**
     * An encoder someone else made and submits, such as an editor viewport's, given as its wgpu
     * handle address.
     */
    public fun borrowed(handle: Long): CommandEncoder = CommandEncoder(handle, null)
  }
}
