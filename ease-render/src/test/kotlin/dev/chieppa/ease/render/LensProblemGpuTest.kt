package dev.chieppa.ease.render

import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.ecs.Phase
import dev.chieppa.ease.ecs.phasedWorld
import dev.chieppa.ease.gpu.GpuContext
import dev.chieppa.ease.gpu.OffscreenTarget
import dev.chieppa.ease.gpu.lit.MeshFrame
import dev.chieppa.ease.platform.PixelSize
import dev.chieppa.ease.render.scene.Camera
import dev.chieppa.ease.render.scene.Mesh
import dev.chieppa.ease.render.scene.MeshShape
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** A camera whose lens cannot be drawn through leaves the target cleared and is reported once. */
class LensProblemGpuTest {
  @Test
  fun `a camera with near 0 draws only the clear color and is reported once`() {
    val snapshot = RenderSnapshot()
    val world = phasedWorld { add(Phase.EXTRACT, ExtractSystem(snapshot)) }
    world.entity {
      it += LocalTransform().apply { z = 10f }
      it += Camera.of().apply { near = 0f }
    }
    world.entity {
      it += LocalTransform()
      it += Mesh.of(MeshShape.CUBE)
    }
    world.update(1f / 60f)
    val reported = mutableListOf<String>()
    val size = PixelSize(32, 32)

    val center =
      GpuContext.open().use { gpu ->
        WorldRenderer(gpu, capacity = 4, problems = { reported += it }).use { renderer ->
          OffscreenTarget(gpu, size).use { offscreen ->
            repeat(FRAMES) {
              gpu.beginFrame()
              val encoder = gpu.frameEncoder()
              renderer.render(snapshot, 1f, offscreen.target, encoder)
              encoder.submit()
            }
            offscreen.read().rgb(size.width / 2, size.height / 2)
          }
        }
      }

    assertEquals(listOf(LensProblem.NEAR.message), reported)
    assertTrue(Rgb.close(center, Rgb.of(MeshFrame.DEFAULT_CLEAR)))
  }

  private companion object {
    const val FRAMES = 3
  }
}
