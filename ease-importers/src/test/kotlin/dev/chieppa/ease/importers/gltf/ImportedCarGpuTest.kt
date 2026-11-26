package dev.chieppa.ease.importers.gltf

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.World
import dev.chieppa.ease.assets.loading.AssetStore
import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.ecs.Phase
import dev.chieppa.ease.ecs.phasedWorld
import dev.chieppa.ease.gpu.GpuContext
import dev.chieppa.ease.gpu.OffscreenTarget
import dev.chieppa.ease.gpu.Pixels
import dev.chieppa.ease.gpu.lit.MeshFrame
import dev.chieppa.ease.platform.PixelSize
import dev.chieppa.ease.render.ExtractSystem
import dev.chieppa.ease.render.RenderSnapshot
import dev.chieppa.ease.render.WorldRenderer
import dev.chieppa.ease.render.scene.Camera
import dev.chieppa.ease.render.scene.DirectionalLight
import dev.chieppa.ease.render.scene.MaterialRef
import dev.chieppa.ease.render.scene.MeshRef
import dev.chieppa.ease.scene.document.SceneDocument
import dev.chieppa.ease.scene.node.NodeKey
import dev.chieppa.ease.scene.node.Parent
import dev.chieppa.ease.scene.runtime.SceneNodes
import dev.chieppa.ease.scene.transform.WorldTransforms
import dev.chieppa.ease.scene.types.SceneTypes
import java.nio.file.Path
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.io.TempDir

/**
 * The Blender-exported car imported, its prefab's nodes spawned with their parents, and drawn
 * offscreen from its side: the body fills the middle, each wheel sits low at its own end, and the
 * background stays clear. Each region is checked by color, not pixel for pixel.
 */
class ImportedCarGpuTest {
  @TempDir lateinit var folder: Path

  private val size = PixelSize(160, 120)
  private val snapshot = RenderSnapshot()

  @Test
  fun `the imported car draws its body and its wheels where its nodes place them`() {
    val project = ImportedProject(folder)
    val car = project.add("models/car_sedan.gltf")
    project.add("models/car_sedan.bin")
    project.add("models/citybits_texture.png")
    val (store, imports) = project.open()
    assertEquals(emptyList(), imports.problems.map { it.toString() })
    val world = phasedWorld {
      add(Phase.INPUT, SceneNodes(TYPES))
      add(Phase.EXTRACT, WorldTransforms())
      add(Phase.EXTRACT, ExtractSystem(snapshot, assets = store))
    }
    spawn(world, store.handle(car, SceneDocument::class).value)
    world.entity {
      // From +x, looking down -x at the car's side, a little above its middle.
      it +=
        LocalTransform().apply {
          x = 2f
          y = 0.15f
          rotationY = HALF_SQRT2
          rotationW = HALF_SQRT2
        }
      it += Camera.of(fieldOfView = 30f)
      it += DirectionalLight.of(intensity = 3f, ambient = 0.2f)
    }
    world.update(DELTA)

    val pixels = draw(store)

    assertEquals(emptyList(), world.system<ExtractSystem>().assetProblems.toList())
    val clear = MeshFrame.DEFAULT_CLEAR
    val body = pixels.rgb(size.width / 2, size.height / 2 - 4)
    assertTrue(!near(body, clear), "the body is not drawn: ${hex(body)}")
    val wheelRow = rowOf(0.0113f)
    val front = pixels.rgb(columnOf(0.2454f), wheelRow)
    val rear = pixels.rgb(columnOf(-0.2562f), wheelRow)
    val between = pixels.rgb(size.width / 2, rowOf(-0.05f))
    assertTrue(!near(front, clear) && !near(rear, clear), "${hex(front)} ${hex(rear)}")
    assertTrue(near(between, clear), "below the body, between the wheels: ${hex(between)}")
    assertTrue(near(pixels.rgb(0, 0), clear) && near(pixels.rgb(size.width - 1, 0), clear))
    assertTrue(CarColors.isTextured(body), "the body is untextured white: ${hex(body)}")
  }

  /** Spawns [prefab]'s nodes as top-level nodes of [world], each with its parent link. */
  private fun spawn(world: World, prefab: SceneDocument) {
    for (node in prefab.nodes.values) {
      world.entity { entity ->
        entity += NodeKey.of(node.id)
        node.parent?.let { entity += Parent(NodeKey.of(it), node.order) }
        node.components.forEach { (name, fields) ->
          TYPES[name]?.let { type -> entity += listOf<Component<*>>(type.decode(fields)) }
        }
      }
    }
  }

  private fun draw(store: AssetStore): Pixels =
    GpuContext.open().use { gpu ->
      WorldRenderer(gpu, capacity = 16, assets = store).use { renderer ->
        OffscreenTarget(gpu, size).use { offscreen ->
          gpu.beginFrame()
          val encoder = gpu.frameEncoder()
          renderer.render(snapshot, 1f, offscreen.target, encoder)
          encoder.submit()
          offscreen.read()
        }
      }
    }

  /** The pixel column of the car's z: its length runs across the view, +z to the left. */
  private fun columnOf(z: Float): Int = (size.width / 2 - z / HALF_HEIGHT * size.height / 2).toInt()

  /** The pixel row of the car's y. */
  private fun rowOf(y: Float): Int =
    (size.height / 2 - (y - 0.15f) / HALF_HEIGHT * size.height / 2).toInt()

  private fun near(rgb: Int, color: dev.chieppa.ease.gpu.Color): Boolean {
    val channels = listOf(color.red, color.green, color.blue).map { (it * 255).toInt() }
    return listOf(16, 8, 0).zip(channels).all { (shift, c) ->
      abs((rgb shr shift and 0xFF) - c) <= 2
    }
  }

  private fun hex(rgb: Int) = "#" + rgb.toString(16).padStart(6, '0')

  private companion object {
    const val DELTA = 1f / 60f
    const val HALF_SQRT2 = 0.70710677f

    /** Half the view's height at distance 2 with a 30 degree field of view: 2 tan 15. */
    const val HALF_HEIGHT = 0.5359f

    val TYPES: SceneTypes = SceneTypes.build {
      component(MeshRef, MeshRef.serializer())
      component(MaterialRef, MaterialRef.serializer())
    }
  }
}
