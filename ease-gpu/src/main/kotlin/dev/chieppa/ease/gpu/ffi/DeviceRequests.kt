package dev.chieppa.ease.gpu.ffi

import dev.chieppa.webgpu.ffi.WGPUDeviceDescriptor
import dev.chieppa.webgpu.ffi.WGPUDeviceLostCallback
import dev.chieppa.webgpu.ffi.WGPUDeviceLostCallbackInfo
import dev.chieppa.webgpu.ffi.WGPUInstanceDescriptor
import dev.chieppa.webgpu.ffi.WGPUInstanceExtras
import dev.chieppa.webgpu.ffi.WGPURequestAdapterCallback
import dev.chieppa.webgpu.ffi.WGPURequestAdapterCallbackInfo
import dev.chieppa.webgpu.ffi.WGPURequestAdapterOptions
import dev.chieppa.webgpu.ffi.WGPURequestDeviceCallback
import dev.chieppa.webgpu.ffi.WGPURequestDeviceCallbackInfo
import dev.chieppa.webgpu.ffi.WGPUUncapturedErrorCallback
import dev.chieppa.webgpu.ffi.WGPUUncapturedErrorCallbackInfo
import dev.chieppa.webgpu.ffi.wgpu_h
import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout.JAVA_INT

/**
 * A wgpu instance. On Windows it compiles Direct3D 12 shaders with FXC, which ships with Windows,
 * rather than whichever DXC is first on the path.
 */
internal fun createInstance(): MemorySegment =
  Arena.ofConfined().use { arena ->
    val extras = arena.struct(WGPUInstanceExtras.layout())
    chain(extras, wgpu_h.WGPUSType_InstanceExtras())
    WGPUInstanceExtras.dx12ShaderCompiler(extras, wgpu_h.WGPUDx12Compiler_Fxc())
    val descriptor = arena.struct(WGPUInstanceDescriptor.layout())
    WGPUInstanceDescriptor.nextInChain(descriptor, extras)
    wgpu_h.wgpuCreateInstance(descriptor).also {
      check(it != MemorySegment.NULL) { "WebGPU is not available" }
    }
  }

/** An adapter on [instance] that can present to [surface], or any adapter when it is null. */
internal fun requestAdapter(instance: MemorySegment, surface: MemorySegment?): MemorySegment =
  Arena.ofConfined().use { arena ->
    val options = arena.struct(WGPURequestAdapterOptions.layout())
    WGPURequestAdapterOptions.featureLevel(options, wgpu_h.WGPUFeatureLevel_Core())
    WGPURequestAdapterOptions.powerPreference(options, wgpu_h.WGPUPowerPreference_HighPerformance())
    WGPURequestAdapterOptions.compatibleSurface(options, surface ?: MemorySegment.NULL)
    val reply = Reply()
    val info = arena.struct(WGPURequestAdapterCallbackInfo.layout())
    WGPURequestAdapterCallbackInfo.mode(info, wgpu_h.WGPUCallbackMode_AllowProcessEvents())
    WGPURequestAdapterCallbackInfo.callback(
      info,
      WGPURequestAdapterCallback.allocate(
        { status, adapter, message, _, _ ->
          if (status == wgpu_h.WGPURequestAdapterStatus_Success()) {
            reply.result = kept(adapter)
          } else {
            reply.failure = readString(message).ifBlank { "status $status" }
          }
        },
        arena,
      ),
    )
    wgpu_h.wgpuInstanceRequestAdapter(arena, instance, options, info)
    reply.await(instance) { "No GPU adapter suits WebGPU: $it" }
  }

/**
 * A device on [adapter], with block-compressed textures (BC1 to BC7) turned on when the adapter has
 * them. [errors] gets every error wgpu reports for it outside a call, and its loss; the callbacks
 * live in [callbacks], which must outlive the device.
 */
internal fun requestDevice(
  instance: MemorySegment,
  adapter: MemorySegment,
  callbacks: Arena,
  errors: (String) -> Unit,
): MemorySegment =
  Arena.ofConfined().use { arena ->
    val descriptor = arena.struct(WGPUDeviceDescriptor.layout())
    val blockCompression = wgpu_h.WGPUFeatureName_TextureCompressionBC()
    if (wgpu_h.wgpuAdapterHasFeature(adapter, blockCompression) != 0) {
      WGPUDeviceDescriptor.requiredFeatureCount(descriptor, 1)
      WGPUDeviceDescriptor.requiredFeatures(
        descriptor,
        arena.allocateFrom(JAVA_INT, blockCompression),
      )
    }
    WGPUUncapturedErrorCallbackInfo.callback(
      WGPUDeviceDescriptor.uncapturedErrorCallbackInfo(descriptor),
      WGPUUncapturedErrorCallback.allocate(
        { _, _, message, _, _ -> errors(readString(message)) },
        callbacks,
      ),
    )
    val loss = WGPUDeviceDescriptor.deviceLostCallbackInfo(descriptor)
    WGPUDeviceLostCallbackInfo.mode(loss, wgpu_h.WGPUCallbackMode_AllowSpontaneous())
    WGPUDeviceLostCallbackInfo.callback(
      loss,
      WGPUDeviceLostCallback.allocate(
        { _, reason, message, _, _ ->
          val released =
            reason == wgpu_h.WGPUDeviceLostReason_Destroyed() ||
              reason == wgpu_h.WGPUDeviceLostReason_CallbackCancelled()
          if (!released) errors("The GPU device was lost: ${readString(message)}")
        },
        callbacks,
      ),
    )
    val reply = Reply()
    val info = arena.struct(WGPURequestDeviceCallbackInfo.layout())
    WGPURequestDeviceCallbackInfo.mode(info, wgpu_h.WGPUCallbackMode_AllowProcessEvents())
    WGPURequestDeviceCallbackInfo.callback(
      info,
      WGPURequestDeviceCallback.allocate(
        { status, device, message, _, _ ->
          if (status == wgpu_h.WGPURequestDeviceStatus_Success()) {
            reply.result = kept(device)
          } else {
            reply.failure = readString(message).ifBlank { "status $status" }
          }
        },
        arena,
      ),
    )
    wgpu_h.wgpuAdapterRequestDevice(arena, adapter, descriptor, info)
    reply.await(instance) { "The GPU device could not be made: $it" }
  }

/** What a wgpu request answered: the object it made, or why it failed. */
private class Reply {
  var result: MemorySegment? = null
  var failure: String? = null

  /**
   * Lets [instance] run callbacks until this request has answered; native requests answer at once.
   */
  fun await(instance: MemorySegment, describe: (String?) -> String): MemorySegment {
    while (result == null && failure == null) wgpu_h.wgpuInstanceProcessEvents(instance)
    return result ?: error(describe(failure))
  }
}
