package dev.chieppa.ease.render

import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.ecs.Phase
import dev.chieppa.ease.ecs.phasedWorld
import dev.chieppa.ease.gpu.GpuContext
import dev.chieppa.ease.gpu.OffscreenTarget
import dev.chieppa.ease.gpu.lit.MaterialTextures
import dev.chieppa.ease.gpu.texture.GpuTexture
import dev.chieppa.ease.gpu.texture.SampledFormat
import dev.chieppa.ease.platform.PixelSize
import dev.chieppa.ease.render.scene.Camera
import dev.chieppa.ease.render.scene.DirectionalLight
import dev.chieppa.ease.render.scene.Material
import dev.chieppa.ease.render.scene.Mesh
import dev.chieppa.ease.render.scene.MeshShape
import dev.chieppa.ease.render.scene.SurfaceTextures
import dev.chieppa.ease.testing.AllocationBudget
import kotlin.test.Test

/**
 * A steady frame, from the start of the frame through recording and submitting a lit scene,
 * allocates nothing on the heap, textured meshes included: descriptors and uploads go through the
 * frame arena and the encoder object is reused. The arena runs unchecked, as in a release build.
 * The target is tiny, so the software rasterizer CI uses keeps up.
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
    GpuContext.open(frameChecks = false).use { gpu ->
      WorldRenderer(gpu, capacity = 8).use { renderer ->
        // Two meshes with a texture set of their own, so the pass switches sets mid-frame.
        val texture = GpuTexture(gpu, 4, 4, SampledFormat.RGBA8UnormSrgb)
        texture.write(0, ByteArray(texture.levelBytes(0)) { -1 })
        val textured = MaterialTextures(gpu, baseColor = texture)
        for (x in listOf(-2f, 2f)) {
          world.entity {
            it += LocalTransform().apply { this.x = x }
            it += Mesh.of(MeshShape.PLANE)
            it += SurfaceTextures(textured)
          }
        }
        world.update(DELTA)
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
        textured.close()
        texture.close()
      }
    }
  }

  private companion object {
    const val DELTA = 1f / 60f
    const val WARMUP_FRAMES = 3_000
    const val MEASURED_FRAMES = 500
  }
}
