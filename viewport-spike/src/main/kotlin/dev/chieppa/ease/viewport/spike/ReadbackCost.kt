package dev.chieppa.ease.viewport.spike

import dev.chieppa.ease.gpu.CommandEncoder
import dev.chieppa.ease.gpu.GpuContext
import dev.chieppa.ease.gpu.OffscreenTarget
import dev.chieppa.ease.gpu.TextureFormat
import dev.chieppa.ease.gpu.TrianglePass
import dev.chieppa.ease.platform.PixelSize
import java.util.Locale

private const val WARMUP_FRAMES = 20
private const val MEASURED_FRAMES = 200
private const val NANOS_PER_MILLI = 1_000_000.0

/** The frame sizes measured: 1080p and 4K. */
private val sizes = listOf(PixelSize(1920, 1080), PixelSize(3840, 2160))

/**
 * Times what the viewport's readback costs per frame, headless: drawing the triangle alone, then
 * drawing it and reading it back, at 1080p and 4K. Each frame waits for the GPU, so the times are
 * an upper bound for Stretch's readback, which overlaps two frames.
 */
public fun main() {
  GpuContext.open().use { gpu ->
    println("Readback cost on ${gpu.description}")
    TrianglePass(gpu).use { triangle ->
      for (size in sizes) {
        OffscreenTarget(gpu, size, TextureFormat.BGRA8Unorm).use { target ->
          val drawOnly = time { draw(gpu, triangle, target, read = false) }
          val withRead = time { draw(gpu, triangle, target, read = true) }
          println(
            "%dx%d: draw %.2f ms, draw and read back %.2f ms, readback %.2f ms a frame"
              .format(
                Locale.ROOT,
                size.width,
                size.height,
                drawOnly,
                withRead,
                withRead - drawOnly,
              )
          )
        }
      }
    }
  }
}

private fun draw(gpu: GpuContext, triangle: TrianglePass, target: OffscreenTarget, read: Boolean) {
  val encoder = CommandEncoder.on(gpu)
  triangle.record(encoder, target.target)
  encoder.submit()
  if (read) target.read() else gpu.awaitIdle()
}

/** Average milliseconds of [frame] over [MEASURED_FRAMES] runs, after [WARMUP_FRAMES]. */
private fun time(frame: () -> Unit): Double {
  repeat(WARMUP_FRAMES) { frame() }
  val start = System.nanoTime()
  repeat(MEASURED_FRAMES) { frame() }
  return (System.nanoTime() - start) / NANOS_PER_MILLI / MEASURED_FRAMES
}
