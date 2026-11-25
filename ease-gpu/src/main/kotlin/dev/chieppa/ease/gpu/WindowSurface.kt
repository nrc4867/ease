package dev.chieppa.ease.gpu

import dev.chieppa.ease.gpu.ffi.Flags
import dev.chieppa.ease.gpu.ffi.FrameCalls
import dev.chieppa.ease.platform.PixelSize
import dev.chieppa.webgpu.ffi.WGPUSurfaceCapabilities
import dev.chieppa.webgpu.ffi.WGPUSurfaceConfiguration
import dev.chieppa.webgpu.ffi.WGPUSurfaceTexture
import dev.chieppa.webgpu.ffi.wgpu_h
import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

/** No texture: the null address. */
private const val NONE = 0L

/**
 * A window's surface: [configure] it for the window's pixel size, then each frame [acquire] a
 * target, draw into it, and [present] it. Presenting waits for the display, so a frame loop runs at
 * its refresh rate without spinning.
 */
public class WindowSurface
internal constructor(
  private val context: GpuContext,
  private val handle: MemorySegment,
  private val adapter: MemorySegment,
) : AutoCloseable {
  /** The format frames are drawn in, the first one this surface supports that Ease renders into. */
  public val format: TextureFormat = supportedFormats().first()

  private var size = PixelSize(0, 0)
  private var current = NONE
  private var currentView = NONE

  /** Where [acquire] asks wgpu for each frame's texture, made once. */
  private val memory = Arena.ofShared()
  private val surfaceTexture = memory.allocate(WGPUSurfaceTexture.layout())

  /** The one target [acquire] hands out, pointed at each frame's texture in turn. */
  private val target = RenderTarget(NONE, size, format)

  /**
   * Sizes the surface's frames to [size]; call it again whenever the window's pixel size changes.
   */
  public fun configure(size: PixelSize) {
    if (size.isEmpty) return
    Arena.ofConfined().use { arena ->
      val configuration = arena.allocate(WGPUSurfaceConfiguration.layout())
      WGPUSurfaceConfiguration.device(configuration, context.device)
      WGPUSurfaceConfiguration.format(configuration, format.native)
      WGPUSurfaceConfiguration.usage(configuration, Flags.TEXTURE_RENDER_ATTACHMENT)
      WGPUSurfaceConfiguration.width(configuration, size.width)
      WGPUSurfaceConfiguration.height(configuration, size.height)
      WGPUSurfaceConfiguration.alphaMode(configuration, wgpu_h.WGPUCompositeAlphaMode_Opaque())
      WGPUSurfaceConfiguration.presentMode(configuration, wgpu_h.WGPUPresentMode_Fifo())
      wgpu_h.wgpuSurfaceConfigure(handle, configuration)
    }
    this.size = size
  }

  /**
   * The next frame to draw into, valid until [present], or null when there is none to draw: the
   * window is minimized, or the surface is out of date and needs [configure] with the window's
   * size.
   */
  public fun acquire(): RenderTarget? {
    check(current == NONE) { "The last frame was not presented" }
    if (size.isEmpty) return null
    val frame = surfaceTexture
    wgpu_h.wgpuSurfaceGetCurrentTexture(handle, frame)
    val texture = frame.get(ValueLayout.JAVA_LONG, WGPUSurfaceTexture.`texture$offset`())
    return when (WGPUSurfaceTexture.status(frame)) {
      wgpu_h.WGPUSurfaceGetCurrentTextureStatus_SuccessOptimal(),
      wgpu_h.WGPUSurfaceGetCurrentTextureStatus_SuccessSuboptimal() -> {
        val view = FrameCalls.createView(texture)
        current = texture
        currentView = view
        target.view = view
        target.size = size
        target
      }
      wgpu_h.WGPUSurfaceGetCurrentTextureStatus_Error() -> throw GpuException("The surface failed")
      else -> {
        if (texture != NONE) FrameCalls.releaseTexture(texture)
        null
      }
    }
  }

  /** Shows the frame [acquire] last returned, once the commands drawing it are submitted. */
  public fun present() {
    check(current != NONE) { "No frame was acquired" }
    wgpu_h.wgpuSurfacePresent(handle)
    FrameCalls.releaseView(currentView)
    FrameCalls.releaseTexture(current)
    current = NONE
    currentView = NONE
  }

  override fun close() {
    if (current != NONE) present()
    wgpu_h.wgpuSurfaceRelease(handle)
    memory.close()
  }

  private fun supportedFormats(): List<TextureFormat> =
    Arena.ofConfined().use { arena ->
      val capabilities = arena.allocate(WGPUSurfaceCapabilities.layout())
      wgpu_h.wgpuSurfaceGetCapabilities(handle, adapter, capabilities)
      val count = WGPUSurfaceCapabilities.formatCount(capabilities)
      val values = WGPUSurfaceCapabilities.formats(capabilities).reinterpret(count * Int.SIZE_BYTES)
      val formats =
        (0 until count).mapNotNull {
          TextureFormat.of(values.getAtIndex(ValueLayout.JAVA_INT, it))
        }
      wgpu_h.wgpuSurfaceCapabilitiesFreeMembers(capabilities)
      formats.ifEmpty { throw GpuException("The surface supports no 8-bit unorm format") }
    }
}
