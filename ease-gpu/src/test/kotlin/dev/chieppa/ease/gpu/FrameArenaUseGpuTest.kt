package dev.chieppa.ease.gpu

import dev.chieppa.ease.gpu.lit.LitMeshPass
import dev.chieppa.ease.platform.PixelSize
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Hosts that never begin a frame, such as an editor viewport, keep drawing; a pass that needs more
 * of the frame arena than it holds is refused when made.
 */
class FrameArenaUseGpuTest {
  @Test
  fun `the triangle records and submits frame after frame without beginning a frame`() {
    GpuContext.open(bytesPerFrame = SMALL_ARENA).use { gpu ->
      TrianglePass(gpu).use { triangle ->
        OffscreenTarget(gpu, PixelSize(8, 8)).use { offscreen ->
          repeat(FRAMES) {
            val encoder = gpu.frameEncoder()
            triangle.record(encoder, offscreen.target, angle = it.toFloat())
            encoder.submit()
          }
          gpu.awaitIdle()
          gpu.checkErrors()
        }
      }
    }
  }

  @Test
  fun `a lit pass with more draws than the frame arena holds is refused, naming the size needed`() {
    GpuContext.open(bytesPerFrame = SMALL_ARENA).use { gpu ->
      val error = assertFailsWith<IllegalArgumentException> { LitMeshPass(gpu, capacity = 100) }

      assertTrue("bytesPerFrame of at least" in error.message.orEmpty(), error.message)
      LitMeshPass(gpu, capacity = 1, jointCapacity = 1).close()
    }
  }

  private companion object {
    /** Room for a few frames' worth of descriptors, were any taken from the arena. */
    const val SMALL_ARENA = 1024L
    const val FRAMES = 500
  }
}
