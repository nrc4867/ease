package dev.chieppa.ease.gpu.ffi

import dev.chieppa.webgpu.ffi.wgpu_h
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.Linker
import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

private val HANDLE: ValueLayout = ValueLayout.JAVA_LONG
private val POINTER: ValueLayout = ValueLayout.ADDRESS
private val INT: ValueLayout = ValueLayout.JAVA_INT

private fun downcall(address: MemorySegment, result: MemoryLayout?, vararg args: MemoryLayout) =
  Linker.nativeLinker()
    .downcallHandle(
      address,
      if (result == null) FunctionDescriptor.ofVoid(*args)
      else FunctionDescriptor.of(result, *args),
    )

/**
 * The wgpu calls a frame makes on objects made every frame (command encoders, their command buffers
 * and render passes), bound so those objects travel as plain addresses. The generated bindings
 * return each new handle as a fresh `MemorySegment`, an object per call; these return and take a
 * `Long`, so recording a frame allocates nothing. Load wgpu-native before first use.
 */
@Suppress("TooManyFunctions") // One function per wgpu call, each a line.
internal object FrameCalls {
  private val createEncoder =
    downcall(wgpu_h.`wgpuDeviceCreateCommandEncoder$address`(), HANDLE, POINTER, POINTER)
  private val finish =
    downcall(wgpu_h.`wgpuCommandEncoderFinish$address`(), HANDLE, HANDLE, POINTER)
  private val releaseEncoder = downcall(wgpu_h.`wgpuCommandEncoderRelease$address`(), null, HANDLE)
  private val releaseCommands = downcall(wgpu_h.`wgpuCommandBufferRelease$address`(), null, HANDLE)
  private val beginPass =
    downcall(wgpu_h.`wgpuCommandEncoderBeginRenderPass$address`(), HANDLE, HANDLE, POINTER)
  private val setPipeline =
    downcall(wgpu_h.`wgpuRenderPassEncoderSetPipeline$address`(), null, HANDLE, POINTER)
  private val setBindGroup =
    downcall(
      wgpu_h.`wgpuRenderPassEncoderSetBindGroup$address`(),
      null,
      HANDLE,
      INT,
      POINTER,
      HANDLE,
      POINTER,
    )
  private val setVertexBuffer =
    downcall(
      wgpu_h.`wgpuRenderPassEncoderSetVertexBuffer$address`(),
      null,
      HANDLE,
      INT,
      POINTER,
      HANDLE,
      HANDLE,
    )
  private val setIndexBuffer =
    downcall(
      wgpu_h.`wgpuRenderPassEncoderSetIndexBuffer$address`(),
      null,
      HANDLE,
      POINTER,
      INT,
      HANDLE,
      HANDLE,
    )
  private val draw =
    downcall(wgpu_h.`wgpuRenderPassEncoderDraw$address`(), null, HANDLE, INT, INT, INT, INT)
  private val drawIndexed =
    downcall(
      wgpu_h.`wgpuRenderPassEncoderDrawIndexed$address`(),
      null,
      HANDLE,
      INT,
      INT,
      INT,
      INT,
      INT,
    )
  private val endPass = downcall(wgpu_h.`wgpuRenderPassEncoderEnd$address`(), null, HANDLE)
  private val releasePass = downcall(wgpu_h.`wgpuRenderPassEncoderRelease$address`(), null, HANDLE)
  private val releaseTexture = downcall(wgpu_h.`wgpuTextureRelease$address`(), null, HANDLE)
  private val releaseView = downcall(wgpu_h.`wgpuTextureViewRelease$address`(), null, HANDLE)
  private val createView =
    downcall(wgpu_h.`wgpuTextureCreateView$address`(), HANDLE, HANDLE, POINTER)

  fun createCommandEncoder(device: MemorySegment): Long =
    createEncoder.invokeExact(device, MemorySegment.NULL) as Long

  fun finish(encoder: Long): Long = finish.invokeExact(encoder, MemorySegment.NULL) as Long

  fun releaseEncoder(encoder: Long) {
    releaseEncoder.invoke(encoder)
  }

  fun releaseCommands(commands: Long) {
    releaseCommands.invoke(commands)
  }

  fun beginRenderPass(encoder: Long, descriptor: MemorySegment): Long =
    beginPass.invokeExact(encoder, descriptor) as Long

  fun setPipeline(pass: Long, pipeline: MemorySegment) {
    setPipeline.invoke(pass, pipeline)
  }

  fun setBindGroup(pass: Long, group: Int, bindGroup: MemorySegment) {
    setBindGroup.invoke(pass, group, bindGroup, 0L, MemorySegment.NULL)
  }

  fun setVertexBuffer(pass: Long, buffer: MemorySegment, bytes: Long, slot: Int = 0) {
    setVertexBuffer.invoke(pass, slot, buffer, 0L, bytes)
  }

  fun setIndexBuffer(pass: Long, buffer: MemorySegment, bytes: Long) {
    setIndexBuffer.invoke(pass, buffer, wgpu_h.WGPUIndexFormat_Uint32(), 0L, bytes)
  }

  fun draw(pass: Long, vertices: Int, instances: Int) {
    draw.invoke(pass, vertices, instances, 0, 0)
  }

  fun drawIndexed(pass: Long, indices: Int, firstInstance: Int) {
    drawIndexed.invoke(pass, indices, 1, 0, 0, firstInstance)
  }

  fun endPass(pass: Long) {
    endPass.invoke(pass)
    releasePass.invoke(pass)
  }

  fun releaseTexture(texture: Long) {
    releaseTexture.invoke(texture)
  }

  fun releaseView(view: Long) {
    releaseView.invoke(view)
  }

  fun createView(texture: Long): Long = createView.invokeExact(texture, MemorySegment.NULL) as Long
}
