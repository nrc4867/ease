package dev.chieppa.ease.sdf

import dev.chieppa.ease.gpu.CommandEncoder
import dev.chieppa.ease.gpu.GpuContext
import dev.chieppa.ease.gpu.OffscreenTarget
import dev.chieppa.ease.gpu.lit.LitMeshPass
import dev.chieppa.ease.gpu.lit.MeshFrame
import dev.chieppa.ease.gpu.mesh.ComputedMesh
import dev.chieppa.ease.math.Mat4Cameras
import dev.chieppa.ease.math.Mat4s
import dev.chieppa.ease.memory.MAT4_FLOATS
import dev.chieppa.ease.platform.PixelSize
import dev.chieppa.ease.sdf.mesh.GpuSurfaceNets
import dev.chieppa.ease.testing.AllocationBudget
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The cave, meshed on the GPU, draws through the lit pass: seen from inside its tunnel the frame is
 * covered by lit rock, with the sunlit floor brighter than walls that face away, and a steady frame
 * of it allocates nothing.
 */
class CaveRenderGpuTest {
  @Test
  fun `the cave fills the view from inside its tunnel with lit surfaces`() {
    val pixels =
      GpuContext.open().use { gpu ->
        GpuSurfaceNets.mesh(gpu, TestCave.field(), TestCave.grid()).use { cave ->
          LitMeshPass(gpu, capacity = 1).use { pass ->
            OffscreenTarget(gpu, SIZE).use { offscreen ->
              gpu.beginFrame()
              val encoder = CommandEncoder.on(gpu)
              pass.record(encoder, offscreen.target, frameOf(cave))
              encoder.submit()
              offscreen.read()
            }
          }
        }
      }

    val clear =
      rgb(MeshFrame.DEFAULT_CLEAR.red, MeshFrame.DEFAULT_CLEAR.green, MeshFrame.DEFAULT_CLEAR.blue)
    var covered = 0
    var brightest = 0
    var darkest = Int.MAX_VALUE
    for (y in 0 until SIZE.height) {
      for (x in 0 until SIZE.width) {
        val pixel = pixels.rgb(x, y)
        if (pixel != clear) covered++
        brightest = maxOf(brightest, brightness(pixel))
        darkest = minOf(darkest, brightness(pixel))
      }
    }
    assertTrue(covered == SIZE.width * SIZE.height, "the rock covers the view: $covered pixels")
    assertTrue(brightest > darkest + CONTRAST, "lit unevenly: $darkest to $brightest")
    assertTrue(
      brightness(pixels.rgb(SIZE.width / 2, SIZE.height - 2)) > darkest + CONTRAST,
      "the floor below the eye is sunlit",
    )
  }

  @Test
  fun `a steady frame of the cave allocates nothing`() {
    GpuContext.open(frameChecks = false).use { gpu ->
      GpuSurfaceNets.mesh(gpu, TestCave.field(), TestCave.grid()).use { cave ->
        LitMeshPass(gpu, capacity = 1).use { pass ->
          OffscreenTarget(gpu, PixelSize(16, 16)).use { offscreen ->
            val frame = frameOf(cave)
            val draw = Runnable {
              gpu.beginFrame()
              val encoder = gpu.frameEncoder()
              pass.record(encoder, offscreen.target, frame)
              encoder.submit()
              gpu.awaitIdle()
            }

            AllocationBudget.assertWithin(0, draw, WARMUP_FRAMES, MEASURED_FRAMES)
            gpu.checkErrors()
          }
        }
      }
    }
  }

  /** A frame drawing [cave] from the tunnel's entrance, looking down it, under a high sun. */
  private fun frameOf(cave: ComputedMesh): MeshFrame {
    val frame = MeshFrame(1)
    val eye = floatArrayOf(-12f, TestCave.FLOOR + 1.6f, 0f)
    val target = floatArrayOf(0f, TestCave.FLOOR + 1f, 0f)
    val view = FloatArray(MAT4_FLOATS)
    val projection = FloatArray(MAT4_FLOATS)
    Mat4Cameras.lookAt(eye, 0, target, 0, floatArrayOf(0f, 1f, 0f), 0, view, 0)
    Mat4Cameras.perspective(
      FIELD_OF_VIEW,
      SIZE.width.toFloat() / SIZE.height,
      0.1f,
      100f,
      projection,
      0,
    )
    Mat4s.mul(projection, 0, view, 0, frame.viewProjection, 0)
    eye.copyInto(frame.cameraPosition)
    frame.lightDirection[0] = 0.3f
    frame.lightDirection[1] = -0.9f
    frame.lightDirection[2] = 0.3f
    frame.lightIntensity = 3f
    frame.ambient.fill(0.05f)
    val draw = frame.add(cave)
    Mat4s.identity(frame.models, draw * MAT4_FLOATS)
    Mat4s.identity(frame.normals, draw * MAT4_FLOATS)
    frame.material(draw, 0.5f, 0.45f, 0.4f, 0.9f, 0f, 0f, 0f, 0f)
    return frame
  }

  private fun rgb(red: Float, green: Float, blue: Float): Int {
    fun channel(value: Float) = Math.round(value * 255)
    return (channel(red) shl 16) or (channel(green) shl 8) or channel(blue)
  }

  private fun brightness(rgb: Int): Int =
    (rgb shr 16 and 0xFF) + (rgb shr 8 and 0xFF) + (rgb and 0xFF)

  private companion object {
    val SIZE = PixelSize(96, 64)
    const val FIELD_OF_VIEW = 1.1f
    const val CONTRAST = 60
    const val WARMUP_FRAMES = 3_000
    const val MEASURED_FRAMES = 500
  }
}
