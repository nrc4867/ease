package dev.chieppa.ease.agent.capture

import dev.chieppa.ease.agent.tools.ToolRefusal
import dev.chieppa.ease.assets.loading.AssetStore
import dev.chieppa.ease.gpu.GpuContext
import dev.chieppa.ease.gpu.GpuException
import dev.chieppa.ease.gpu.OffscreenTarget
import dev.chieppa.ease.gpu.Pixels
import dev.chieppa.ease.platform.PixelSize
import dev.chieppa.ease.render.RenderSnapshot
import dev.chieppa.ease.render.WorldRenderer
import dev.chieppa.ease.render.extractFrom
import dev.chieppa.ease.runtime.HostedWorld

/**
 * Draws a world offscreen and reads the frame back. The GPU device is opened on the first capture,
 * through [open], and kept; when it cannot be opened, as on a machine with no WebGPU adapter, every
 * capture refuses with the reason. Mesh and material assets, and their textures, load through
 * [assets]. Use it from the world's thread only.
 */
public class Capturer(
  private val open: () -> GpuContext = { GpuContext.open() },
  private val assets: AssetStore? = null,
) : AutoCloseable {
  private var gpu: GpuContext? = null
  private var renderer: WorldRenderer? = null
  private var unavailable: String? = null
  private val snapshot = RenderSnapshot()

  /** [world] through its camera, [size] pixels: every mesh where it is now, the world only read. */
  public fun capture(world: HostedWorld, size: PixelSize): Pixels {
    val context = gpu ?: start()
    val drawer = checkNotNull(renderer)
    snapshot.extractFrom(world.world, assets)
    if (snapshot.count > drawer.capacity) {
      throw ToolRefusal(
        "${snapshot.count} meshes are more than a capture draws (${drawer.capacity})"
      )
    }
    return OffscreenTarget(context, size).use { offscreen ->
      context.beginFrame()
      val encoder = context.frameEncoder()
      drawer.render(snapshot, 1f, offscreen.target, encoder)
      encoder.submit()
      offscreen.read()
    }
  }

  private fun start(): GpuContext {
    unavailable?.let { throw ToolRefusal(it) }
    val context =
      try {
        open()
      } catch (failure: IllegalStateException) {
        refuse(failure)
      } catch (failure: GpuException) {
        refuse(failure)
      } catch (failure: LinkageError) {
        refuse(failure)
      }
    gpu = context
    renderer = WorldRenderer(context, assets = assets)
    return context
  }

  private fun refuse(failure: Throwable): Nothing {
    val reason = "Capture needs a GPU and none could be opened: ${failure.message ?: failure}"
    unavailable = reason
    throw ToolRefusal(reason)
  }

  override fun close() {
    renderer?.close()
    gpu?.close()
    renderer = null
    gpu = null
  }
}
