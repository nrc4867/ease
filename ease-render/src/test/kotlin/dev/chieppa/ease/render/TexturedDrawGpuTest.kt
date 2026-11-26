package dev.chieppa.ease.render

import com.github.quillraven.fleks.World
import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.ecs.Phase
import dev.chieppa.ease.ecs.phasedWorld
import dev.chieppa.ease.gpu.GpuContext
import dev.chieppa.ease.gpu.OffscreenTarget
import dev.chieppa.ease.gpu.Pixels
import dev.chieppa.ease.gpu.lit.MaterialTextures
import dev.chieppa.ease.gpu.mesh.MeshData
import dev.chieppa.ease.gpu.texture.GpuTexture
import dev.chieppa.ease.platform.PixelSize
import dev.chieppa.ease.platform.images.DecodedImage
import dev.chieppa.ease.render.scene.Camera
import dev.chieppa.ease.render.scene.CustomMesh
import dev.chieppa.ease.render.scene.DirectionalLight
import dev.chieppa.ease.render.scene.Material
import dev.chieppa.ease.render.scene.SurfaceTextures
import dev.chieppa.ease.render.textures.TextureUpload
import dev.chieppa.ease.textures.CookedTexture
import dev.chieppa.ease.textures.TextureEncoding
import dev.chieppa.ease.textures.TextureUsage
import dev.chieppa.ease.textures.importing.TextureCooker
import dev.chieppa.ease.textures.importing.TextureSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * A square filling most of the view, 10 units in front of the camera and lit head-on by the sun,
 * drawn with cooked textures: its pixels show what the textures hold.
 */
class TexturedDrawGpuTest {
  private val size = PixelSize(96, 96)
  private val snapshot = RenderSnapshot()
  private val world: World = phasedWorld { add(Phase.EXTRACT, ExtractSystem(snapshot)) }

  init {
    world.entity {
      it += LocalTransform().apply { z = 10f }
      it += Camera.of(fieldOfView = 60f)
    }
    world.entity {
      it += LocalTransform()
      it += DirectionalLight.of(intensity = 3f, ambient = 0.05f)
    }
  }

  /** An 8 by 8 image, the left half [left] and the right half [right], as RGBA bytes. */
  private fun halves(left: Int, right: Int): DecodedImage.Bytes {
    val bytes = ByteArray(8 * 8 * 4)
    for (pixel in 0 until 64) {
      val rgb = if (pixel % 8 < 4) left else right
      bytes[pixel * 4] = (rgb shr 16).toByte()
      bytes[pixel * 4 + 1] = (rgb shr 8).toByte()
      bytes[pixel * 4 + 2] = rgb.toByte()
      bytes[pixel * 4 + 3] = -1
    }
    return DecodedImage.Bytes(8, 8, bytes)
  }

  private fun cook(image: DecodedImage, usage: TextureUsage): CookedTexture =
    TextureCooker.cook(image, TextureSettings(usage = usage, mipmaps = false))

  @Test
  fun `a BC7 base color texture shows on both halves, with and without block compression`() {
    val texture = cook(halves(0xE01010, 0x1010E0), TextureUsage.COLOR_SRGB)
    assertEquals(TextureEncoding.BC7_SRGB, texture.encoding)
    for (decode in listOf(false, true)) {
      val pixels = draw { gpu ->
        mapOf("base" to TextureUpload.upload(gpu, texture, decode || !gpu.supportsBlockCompression))
      }
      val left = pixels.rgb(size.width / 4, size.height / 2)
      val right = pixels.rgb(size.width * 3 / 4, size.height / 2)
      assertTrue(red(left) > 2 * blue(left), "decode $decode left ${hex(left)}")
      assertTrue(blue(right) > 2 * red(right), "decode $decode right ${hex(right)}")
    }
  }

  @Test
  fun `one texture set draws in two renderers on the same device`() {
    val texture = cook(halves(0xE01010, 0x1010E0), TextureUsage.COLOR_SRGB)
    val frames = drawWith(2) { gpu -> mapOf("base" to TextureUpload.upload(gpu, texture)) }
    for (pixels in frames) {
      val left = pixels.rgb(size.width / 4, size.height / 2)
      assertTrue(red(left) > 2 * blue(left), hex(left))
    }
  }

  @Test
  fun `a BC5 normal map turns the lit square's right half away from the sun`() {
    // Left: a flat normal. Right: x = +0.87, so the surface faces mostly sideways.
    val texture = cook(halves(0x8080FF, 0xF08040), TextureUsage.NORMAL)
    assertEquals(TextureEncoding.BC5_UNORM, texture.encoding)
    val pixels = draw { gpu -> mapOf("normal" to TextureUpload.upload(gpu, texture)) }
    val flat = pixels.rgb(size.width / 4, size.height / 2)
    val bent = pixels.rgb(size.width * 3 / 4, size.height / 2)
    assertTrue(red(flat) > red(bent) + 40, "flat ${hex(flat)} vs bent ${hex(bent)}")
  }

  private fun draw(textures: (GpuContext) -> Map<String, GpuTexture>): Pixels =
    drawWith(1, textures).single()

  /**
   * The square drawn with the textures [textures] uploads, once by each of [renderers] renderers on
   * one device, all sharing one texture set.
   */
  private fun drawWith(
    renderers: Int,
    textures: (GpuContext) -> Map<String, GpuTexture>,
  ): List<Pixels> =
    GpuContext.open().use { gpu ->
      val uploaded = textures(gpu)
      val set = MaterialTextures(gpu, baseColor = uploaded["base"], normal = uploaded["normal"])
      val drawn = List(renderers) { WorldRenderer(gpu, capacity = 4) }
      MeshData()
        .use { square -> drawSquare(gpu, drawn, square, set) }
        .also {
          gpu.checkErrors()
          drawn.forEach(WorldRenderer::close)
          set.close()
          uploaded.values.forEach(GpuTexture::close)
        }
    }

  /** A frame of [square] drawn with [set] by each of [renderers], then removed from the world. */
  private fun drawSquare(
    gpu: GpuContext,
    renderers: List<WorldRenderer>,
    square: MeshData,
    set: MaterialTextures,
  ): List<Pixels> {
    square(square)
    val drawn = world.entity {
      it += LocalTransform()
      it += CustomMesh(square)
      it += Material.of(1f, 1f, 1f, roughness = 1f)
      it += SurfaceTextures(set)
    }
    world.update(DELTA)
    return renderers
      .map { renderer ->
        OffscreenTarget(gpu, size).use { offscreen ->
          gpu.beginFrame()
          val encoder = gpu.frameEncoder()
          renderer.render(snapshot, 1f, offscreen.target, encoder)
          encoder.submit()
          offscreen.read()
        }
      }
      .also { with(world) { drawn.remove() } }
  }

  /** A 10 by 10 square facing +z, its texture's top-left corner at the top left. */
  private fun square(mesh: MeshData) {
    mesh.vertex(-5f, -5f, 0f, 0f, 0f, 1f, 0f, 1f)
    mesh.vertex(5f, -5f, 0f, 0f, 0f, 1f, 1f, 1f)
    mesh.vertex(5f, 5f, 0f, 0f, 0f, 1f, 1f, 0f)
    mesh.vertex(-5f, 5f, 0f, 0f, 0f, 1f, 0f, 0f)
    mesh.triangle(0, 1, 2)
    mesh.triangle(0, 2, 3)
    mesh.commit()
  }

  private fun red(rgb: Int) = rgb shr 16 and 0xFF

  private fun blue(rgb: Int) = rgb and 0xFF

  private fun hex(rgb: Int) = "#" + rgb.toString(16).padStart(6, '0')

  private companion object {
    const val DELTA = 1f / 60f
  }
}
