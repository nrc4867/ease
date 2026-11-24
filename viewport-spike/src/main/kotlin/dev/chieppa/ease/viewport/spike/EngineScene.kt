package dev.chieppa.ease.viewport.spike

import dev.chieppa.ease.gpu.CommandEncoder
import dev.chieppa.ease.gpu.GpuContext
import dev.chieppa.ease.gpu.RenderTarget
import dev.chieppa.ease.gpu.TextureFormat
import dev.chieppa.ease.gpu.TrianglePass
import dev.chieppa.ease.platform.PixelSize
import dev.chieppa.stretch.gpu.GpuContext as StretchGpuContext
import dev.chieppa.stretch.gpu.GpuFrame
import dev.chieppa.stretch.gpu.GpuScene
import dev.chieppa.stretch.gpu.nativeHandles
import dev.chieppa.stretch.gpu.webgpu.TextureFormat as StretchTextureFormat

/** How far the triangle turns each frame, in radians. */
private const val TURN_PER_FRAME = 0.02f

/**
 * The engine inside a Stretch viewport: Ease's [TrianglePass] recorded with Ease's own bindings
 * onto the device, encoder and output view Stretch's `GpuSurface` owns. Nothing is copied between
 * the two; Stretch reads the frame back for Compose as it does any scene. [rate] counts the frames.
 */
internal class EngineScene(stretch: StretchGpuContext, private val rate: FrameRate) :
  GpuScene<Unit> {
  private val engine =
    stretch.nativeHandles.let { GpuContext.shared(it.instance, it.device, stretch.description) }
  private val format =
    when (stretch.format) {
      StretchTextureFormat.BGRA8Unorm -> TextureFormat.BGRA8Unorm
      StretchTextureFormat.RGBA8Unorm -> TextureFormat.RGBA8Unorm
      else -> error("The engine does not render into ${stretch.format}")
    }
  private val triangle = TrianglePass(engine)
  private var frame = 0

  override fun render(frame: GpuFrame, state: Unit): Boolean {
    val handles = frame.nativeHandles
    val target = RenderTarget.borrowed(handles.output, PixelSize(frame.width, frame.height), format)
    triangle.record(CommandEncoder.borrowed(handles.encoder), target, this.frame * TURN_PER_FRAME)
    this.frame++
    rate.tick()
    return true
  }

  override fun close() {
    triangle.close()
    engine.close()
  }
}
