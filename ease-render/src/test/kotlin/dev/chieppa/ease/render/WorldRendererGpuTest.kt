package dev.chieppa.ease.render

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.ecs.Phase
import dev.chieppa.ease.ecs.phasedWorld
import dev.chieppa.ease.gpu.GpuContext
import dev.chieppa.ease.gpu.OffscreenTarget
import dev.chieppa.ease.gpu.Pixels
import dev.chieppa.ease.gpu.lit.MeshFrame
import dev.chieppa.ease.gpu.mesh.MeshData
import dev.chieppa.ease.platform.PixelSize
import dev.chieppa.ease.render.scene.Camera
import dev.chieppa.ease.render.scene.CustomMesh
import dev.chieppa.ease.render.scene.DirectionalLight
import dev.chieppa.ease.render.scene.Material
import dev.chieppa.ease.render.scene.Mesh
import dev.chieppa.ease.render.scene.MeshShape
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Small worlds drawn offscreen through a camera 10 units up +z looking at the origin, lit by a sun
 * shining the way the camera looks unless a test turns it, then probed pixel by pixel.
 */
class WorldRendererGpuTest {
  private val size = PixelSize(160, 120)
  private val snapshot = RenderSnapshot()
  private val world: World = phasedWorld { add(Phase.EXTRACT, ExtractSystem(snapshot)) }
  private val sun: Entity

  init {
    world.entity {
      it += LocalTransform().apply { z = 10f }
      it += Camera.of(fieldOfView = 60f)
    }
    sun = world.entity {
      it += LocalTransform()
      it += DirectionalLight.of(intensity = 3f, ambient = 0.1f)
    }
  }

  private fun cube(x: Float, z: Float, red: Float, green: Float, blue: Float): Entity =
    world.entity {
      it +=
        LocalTransform().apply {
          this.x = x
          this.z = z
        }
      it += Mesh.of(MeshShape.CUBE)
      it += Material.of(red, green, blue, roughness = 0.9f)
    }

  @Test
  fun `a lit cube shows its color at the center and the corners keep the clear color`() {
    cube(0f, 0f, red = 0.9f, green = 0.1f, blue = 0.1f)
    world.update(DELTA)

    val pixels = draw(alpha = 1f)

    val center = pixels.rgb(size.width / 2, size.height / 2)
    assertTrue(red(center) > 2 * green(center) && red(center) > 2 * blue(center), hex(center))
    val clear = Rgb.of(MeshFrame.DEFAULT_CLEAR)
    assertTrue(Rgb.close(pixels.rgb(0, 0), clear), hex(pixels.rgb(0, 0)))
    assertTrue(Rgb.close(pixels.rgb(size.width - 1, size.height - 1), clear))
  }

  @Test
  fun `the depth buffer keeps a nearer cube in front of one drawn after it`() {
    cube(0f, 2f, red = 0.1f, green = 0.9f, blue = 0.1f)
    cube(0f, 0f, red = 0.9f, green = 0.1f, blue = 0.1f)
    world.update(DELTA)

    val center = draw(alpha = 1f).rgb(size.width / 2, size.height / 2)

    assertTrue(green(center) > 2 * red(center), "the far cube shows through: ${hex(center)}")
  }

  @Test
  fun `a face turned away from the sun is darker than one facing it`() {
    cube(0f, 0f, red = 0.8f, green = 0.8f, blue = 0.8f)
    world.update(DELTA)
    val facing = draw(alpha = 1f).rgb(size.width / 2, size.height / 2)

    with(world) {
      // A quarter turn about y: the sun shines along -x, across the face the camera sees.
      sun[LocalTransform].rotationY = HALF_SQRT2
      sun[LocalTransform].rotationW = HALF_SQRT2
    }
    world.update(DELTA)
    val across = draw(alpha = 1f).rgb(size.width / 2, size.height / 2)

    assertTrue(red(facing) > red(across) + LIT_MARGIN, "${hex(facing)} vs ${hex(across)}")
  }

  @Test
  fun `a mesh is drawn between its previous and current tick by alpha`() {
    val moving = cube(-3f, 0f, red = 0.9f, green = 0.9f, blue = 0.1f)
    world.update(DELTA)
    with(world) { moving[LocalTransform].x = 3f }
    world.update(DELTA)

    val pixels = draw(alpha = 0.5f)

    val middle = size.height / 2
    assertTrue(red(pixels.rgb(size.width / 2, middle)) > CLEAR_RED, "not at the center")
    assertTrue(Rgb.close(pixels.rgb(pixelOf(-3f), middle), Rgb.of(MeshFrame.DEFAULT_CLEAR)))
    assertTrue(Rgb.close(pixels.rgb(pixelOf(3f), middle), Rgb.of(MeshFrame.DEFAULT_CLEAR)))
  }

  @Test
  fun `a runtime mesh built in code is drawn`() {
    MeshData().use { triangle ->
      triangle.vertex(-2f, -2f, 0f, 0f, 0f, 1f, 0f, 0f)
      triangle.vertex(2f, -2f, 0f, 0f, 0f, 1f, 1f, 0f)
      triangle.vertex(0f, 2f, 0f, 0f, 0f, 1f, 0.5f, 1f)
      triangle.triangle(0, 1, 2)
      triangle.commit()
      world.entity {
        it += LocalTransform()
        it += CustomMesh(triangle)
        it += Material.of(0.1f, 0.1f, 0.9f)
      }
      world.update(DELTA)

      val center = draw(alpha = 1f).rgb(size.width / 2, size.height / 2)

      assertTrue(blue(center) > red(center) + LIT_MARGIN, hex(center))
    }
  }

  private fun draw(alpha: Float): Pixels =
    GpuContext.open().use { gpu ->
      WorldRenderer(gpu, capacity = 8).use { renderer ->
        OffscreenTarget(gpu, size).use { offscreen ->
          gpu.beginFrame()
          val encoder = gpu.frameEncoder()
          renderer.render(snapshot, alpha, offscreen.target, encoder)
          encoder.submit()
          offscreen.read()
        }
      }
    }

  /** The pixel column of world x on the plane z = 0, seen 10 units away with a 60 degree view. */
  private fun pixelOf(x: Float): Int {
    val halfHeight = 10f / sqrt(3f)
    return (size.width / 2 + x / halfHeight * size.height / 2).toInt()
  }

  private fun red(rgb: Int) = rgb shr 16 and 0xFF

  private fun green(rgb: Int) = rgb shr 8 and 0xFF

  private fun blue(rgb: Int) = rgb and 0xFF

  private fun hex(rgb: Int) = "#" + rgb.toString(16).padStart(6, '0')

  private companion object {
    const val DELTA = 1f / 60f
    const val HALF_SQRT2 = 0.70710677f
    const val LIT_MARGIN = 40
    const val CLEAR_RED = 40
  }
}
