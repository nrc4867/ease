package dev.chieppa.ease.render

import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.ecs.Phase
import dev.chieppa.ease.ecs.phasedWorld
import dev.chieppa.ease.gpu.GpuContext
import dev.chieppa.ease.gpu.OffscreenTarget
import dev.chieppa.ease.platform.PixelSize
import dev.chieppa.ease.render.scene.Camera
import dev.chieppa.ease.render.scene.DirectionalLight
import dev.chieppa.ease.render.scene.Material
import dev.chieppa.ease.render.scene.Mesh
import dev.chieppa.ease.render.scene.MeshShape
import dev.chieppa.ease.testing.AllocationBudget
import kotlin.test.Test

/**
 * A steady frame, from the start of the frame through recording and submitting a lit scene,
 * allocates nothing on the heap: descriptors and uploads go through the frame arena and the encoder
 * object is reused. The arena runs unchecked, as in a release build. The target is tiny, so the
 * software rasterizer CI uses keeps up.
 */
class FrameAllocationGpuTest {
  @Test
  fun `a steady frame of lit meshes allocates nothing`() {
    val snapshot = RenderSnapshot()
    val world = phasedWorld { add(Phase.EXTRACT, ExtractSystem(snapshot)) }
    world.entity {
      it += LocalTransform().apply { z = 8f }
      it += Camera.of()
    }
    world.entity {
      it += LocalTransform()
      it += DirectionalLight.of()
    }
    MeshShape.entries.forEachIndexed { index, shape ->
      world.entity {
        it += LocalTransform().apply { x = index - 1f }
        it += Mesh.of(shape)
        it += Material.of(0.5f, 0.5f, 0.5f)
      }
    }
    world.update(DELTA)

    GpuContext.open(frameChecks = false).use { gpu ->
      WorldRenderer(gpu, capacity = 8).use { renderer ->
        OffscreenTarget(gpu, PixelSize(16, 16)).use { offscreen ->
          val frame = Runnable {
            gpu.beginFrame()
            val encoder = gpu.frameEncoder()
            renderer.render(snapshot, 0.5f, offscreen.target, encoder)
            encoder.submit()
            gpu.awaitIdle()
          }

          AllocationBudget.assertWithin(0, frame, WARMUP_FRAMES, MEASURED_FRAMES)
          gpu.checkErrors()
        }
      }
    }
  }

  private companion object {
    const val DELTA = 1f / 60f
    const val WARMUP_FRAMES = 3_000
    const val MEASURED_FRAMES = 500
  }
}
