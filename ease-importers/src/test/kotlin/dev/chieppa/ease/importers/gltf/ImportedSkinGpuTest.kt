package dev.chieppa.ease.importers.gltf

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.World
import dev.chieppa.ease.animation.Animation
import dev.chieppa.ease.animation.assets.AnimatorAsset
import dev.chieppa.ease.animation.assets.AnimatorFormat
import dev.chieppa.ease.animation.assets.AnimatorState
import dev.chieppa.ease.animation.runtime.AnimationAssets
import dev.chieppa.ease.animation.scene.Animator
import dev.chieppa.ease.animation.scene.SkinRef
import dev.chieppa.ease.assets.Guid
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
import dev.chieppa.ease.scene.node.LocalId
import dev.chieppa.ease.scene.node.NodeKey
import dev.chieppa.ease.scene.node.Parent
import dev.chieppa.ease.scene.runtime.SceneNodes
import dev.chieppa.ease.scene.slots.AssetRef
import dev.chieppa.ease.scene.transform.WorldTransforms
import dev.chieppa.ease.scene.types.SceneTypes
import dev.chieppa.ease.testing.AllocationBudget
import java.nio.file.Path
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.io.TempDir

/**
 * Khronos' SimpleSkin (a strip two units tall whose upper half is skinned to a second joint)
 * imported and drawn on the GPU from in front: in its bind pose it stands straight up; played 1.25
 * seconds into its clip, where the joint is turned a quarter turn, its upper half lies bent to the
 * left, so the pixels above the joint empty and those beside it fill.
 */
class ImportedSkinGpuTest {
  @TempDir lateinit var folder: Path

  private val size = PixelSize(SIDE, SIDE)

  @Test
  fun `the skinned strip draws bent by its pose, not in its bind pose`() {
    val project = ImportedProject(folder)
    val strip = project.add("khronos/SimpleSkin.gltf")
    val animator = project.write("strip.animator", animatorText(strip.named("clip0.clip")))
    val (store, imports) = project.open()
    assertEquals(emptyList(), imports.problems.map { it.toString() })

    val bind = draw(store, strip, animator = null)
    val posed = draw(store, strip, animator)

    val clear = MeshFrame.DEFAULT_CLEAR
    assertTrue(!near(bind.rgb(at(0f), row(ABOVE_JOINT)), clear), "bind pose: upper half drawn")
    assertTrue(near(bind.rgb(at(BESIDE), row(BESIDE_HEIGHT)), clear), "bind pose: side empty")
    assertTrue(near(posed.rgb(at(0f), row(ABOVE_JOINT)), clear), "posed: upper half moved away")
    assertTrue(!near(posed.rgb(at(BESIDE), row(BESIDE_HEIGHT)), clear), "posed: bent to the side")
  }

  private fun animatorText(clip: Guid): String =
    AnimatorFormat.write(AnimatorAsset(states = listOf(AnimatorState("bend", clip = clip))))
      .toString(Charsets.UTF_8)

  @Test
  fun `steady ticks and frames of the playing skinned strip allocate nothing`() {
    val project = ImportedProject(folder)
    val strip = project.add("khronos/SimpleSkin.gltf")
    val animator = project.write("strip.animator", animatorText(strip.named("clip0.clip")))
    val (store, _) = project.open()
    val snapshot = RenderSnapshot()
    val world = placed(store, strip, animator, snapshot)
    GpuContext.open(frameChecks = false).use { gpu ->
      WorldRenderer(gpu, capacity = 16, assets = store).use { renderer ->
        OffscreenTarget(gpu, PixelSize(16, 16)).use { offscreen ->
          val tickAndFrame = Runnable {
            world.update(DELTA)
            gpu.beginFrame()
            val encoder = gpu.frameEncoder()
            renderer.render(snapshot, 0.5f, offscreen.target, encoder)
            encoder.submit()
            gpu.awaitIdle()
          }

          AllocationBudget.assertWithin(0, tickAndFrame, WARMUP_FRAMES, MEASURED_FRAMES)
          gpu.checkErrors()
          assertTrue(snapshot.skins.joints > 0, "the strip was drawn unskinned")
        }
      }
    }
  }

  /** The strip's prefab placed under one instance node, ticked [TICKS] times, then drawn. */
  private fun draw(store: AssetStore, strip: Guid, animator: Guid?): Pixels {
    val snapshot = RenderSnapshot()
    val world = placed(store, strip, animator, snapshot)
    repeat(TICKS) { world.update(DELTA) }
    return GpuContext.open().use { gpu ->
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
  }

  /**
   * A world holding the strip's prefab placed under one instance node, played by [animator] when
   * given, seen by a camera in front of it and extracted into [snapshot].
   */
  private fun placed(
    store: AssetStore,
    strip: Guid,
    animator: Guid?,
    snapshot: RenderSnapshot,
  ): World {
    val world = phasedWorld {
      add(Phase.INPUT, SceneNodes(TYPES))
      Animation.addTo(this, AnimationAssets.of(store))
      add(Phase.EXTRACT, WorldTransforms())
      add(Phase.EXTRACT, ExtractSystem(snapshot, assets = store))
    }
    world.entity {
      it += PLACEMENT
      it += LocalTransform()
      animator?.let { guid -> it += Animator(AssetRef(guid)) }
    }
    spawn(world, store.handle(strip, SceneDocument::class).value)
    world.entity {
      it +=
        LocalTransform().apply {
          y = 1f
          z = CAMERA_DISTANCE
        }
      it += Camera.of(fieldOfView = FIELD_OF_VIEW)
      it += DirectionalLight.of(intensity = 3f, ambient = 0.3f)
    }
    return world
  }

  /** Spawns [prefab]'s nodes under [PLACEMENT], as placing it there would. */
  private fun spawn(world: World, prefab: SceneDocument) {
    for (node in prefab.nodes.values) {
      world.entity { entity ->
        entity += PLACEMENT.child(node.id)
        entity += Parent(node.parent?.let(PLACEMENT::child) ?: PLACEMENT, node.order)
        node.components.forEach { (name, fields) ->
          TYPES[name]?.let { type -> entity += listOf<Component<*>>(type.decode(fields)) }
        }
      }
    }
  }

  /** The pixel column of world x, seen from +z with the camera at x = 0. */
  private fun at(x: Float): Int = (SIDE / 2 + x * PIXELS_PER_UNIT).toInt()

  /** The pixel row of world y, the camera at y = 1. */
  private fun row(y: Float): Int = (SIDE / 2 - (y - 1f) * PIXELS_PER_UNIT).toInt()

  private fun near(rgb: Int, color: dev.chieppa.ease.gpu.Color): Boolean {
    val channels = listOf(color.red, color.green, color.blue).map { (it * 255).toInt() }
    return listOf(16, 8, 0).zip(channels).all { (shift, c) ->
      abs((rgb shr shift and 0xFF) - c) <= 2
    }
  }

  private companion object {
    const val SIDE = 120
    const val WARMUP_FRAMES = 2_000
    const val MEASURED_FRAMES = 300
    const val DELTA = 1f / 60f

    /** 1.25 seconds: the clip holds its joint a quarter turn over from 1 to 1.5 seconds. */
    const val TICKS = 75
    const val FIELD_OF_VIEW = 60f
    const val CAMERA_DISTANCE = 4f

    /** Pixels a world unit at the strip's distance: half the view over 4 tan 30. */
    const val PIXELS_PER_UNIT = SIDE / 2 / 2.3094f
    const val ABOVE_JOINT = 1.75f
    const val BESIDE = -0.8f
    const val BESIDE_HEIGHT = 1.2f

    val PLACEMENT: NodeKey = NodeKey.of(LocalId(0x5EED))

    val TYPES: SceneTypes = SceneTypes.build {
      component(MeshRef, MeshRef.serializer())
      component(MaterialRef, MaterialRef.serializer())
      component(SkinRef, SkinRef.serializer())
    }
  }
}
