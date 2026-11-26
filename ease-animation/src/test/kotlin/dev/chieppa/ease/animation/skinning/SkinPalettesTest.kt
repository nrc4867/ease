package dev.chieppa.ease.animation.skinning

import dev.chieppa.ease.animation.AnimatedWorld
import dev.chieppa.ease.animation.TestRig
import dev.chieppa.ease.animation.scene.SkinRef
import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.memory.MAT4_FLOATS
import dev.chieppa.ease.scene.node.LocalId
import dev.chieppa.ease.scene.node.NodeKey
import dev.chieppa.ease.scene.node.NodeName
import dev.chieppa.ease.scene.node.OrderKey
import dev.chieppa.ease.scene.node.Parent
import dev.chieppa.ease.scene.slots.AssetRef
import dev.chieppa.ease.scene.transform.WorldTransforms
import kotlin.test.Test
import kotlin.test.assertEquals

/** Skinned meshes' joint matrices follow the nodes their skin's joints name. */
class SkinPalettesTest {
  private val rigKey = NodeKey.of(LocalId(1))

  @Test
  fun `a joint node replaced by a new one is followed, though the node count is unchanged`() =
    AnimatedWorld().use { animated ->
      val world = animated.world
      TestRig.place(world)
      val mesh = world.entity {
        it += rigKey.child(LocalId(MESH))
        it += Parent(rigKey, OrderKey("z"))
        it += LocalTransform()
        it += SkinRef(AssetRef(TestRig.SKIN))
      }
      val palettes = SkinPalettes(TestRig.assets())
      val palette = FloatArray(2 * MAT4_FLOATS)
      val worlds = world.system<WorldTransforms>()
      animated.step()
      palettes.write(world, mesh, worlds, palette, 0)

      val legKey = rigKey.child(TestRig.LEG_LEFT)
      val legs = world.family { all(NodeName) }
      with(world) { legs.first { it[NodeName].name == "legLeft" }.remove() }
      world.entity {} // takes the removed leg's id, so the new leg gets another
      world.entity {
        it += legKey
        it += NodeName("legLeft")
        it += Parent(rigKey.child(TestRig.HIPS), OrderKey("m"))
        it += LocalTransform().apply { x = MOVED_X }
      }
      animated.step()
      val written = palettes.write(world, mesh, worlds, palette, 0)

      assertEquals(2, written)
      assertEquals(MOVED_X, palette[TRANSLATION_X], 1e-5f)
    }

  private companion object {
    const val MESH = 0x20L
    const val MOVED_X = 5f
    const val TRANSLATION_X = 12
  }
}
