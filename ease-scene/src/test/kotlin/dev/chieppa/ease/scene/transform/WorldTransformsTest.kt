package dev.chieppa.ease.scene.transform

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.ecs.Phase
import dev.chieppa.ease.ecs.phasedWorld
import dev.chieppa.ease.scene.Scenes
import dev.chieppa.ease.scene.node.LocalId
import dev.chieppa.ease.scene.node.NodeKey
import dev.chieppa.ease.scene.node.OrderKey
import dev.chieppa.ease.scene.node.Parent
import dev.chieppa.ease.scene.runtime.SceneNodes
import dev.chieppa.ease.testing.AllocationBudget
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals

/** Children placed by their parents, worked out each tick. */
class WorldTransformsTest {
  private val world: World = phasedWorld {
    add(Phase.INPUT, SceneNodes(Scenes.types))
    add(Phase.EXTRACT, WorldTransforms())
  }
  private val transforms = world.system<WorldTransforms>()
  private val out = FloatArray(WORLD_TRANSFORM_FLOATS)

  private fun node(id: Long, parent: Long?, place: LocalTransform.() -> Unit): Entity =
    world.entity {
      it += NodeKey.of(LocalId(id))
      it += LocalTransform().apply(place)
      if (parent != null) it += Parent(NodeKey.of(LocalId(parent)), OrderKey("x"))
    }

  private fun worldOf(entity: Entity): FloatArray {
    transforms.write(entity, out, 0)
    return out
  }

  @Test
  fun `a child is moved, turned and scaled by its parents`() {
    val half = sqrt(0.5f)
    node(1, null) {
      x = 10f
      scaleX = 2f
      scaleY = 2f
      scaleZ = 2f
    }
    node(2, 1) {
      x = 1f
      rotationZ = half
      rotationW = half
    }
    val grandchild = node(3, 2) { x = 1f }

    world.update(1f)
    val placed = worldOf(grandchild)

    assertEquals(12f, placed[0], 1e-5f)
    assertEquals(2f, placed[1], 1e-5f)
    assertEquals(half, placed[5], 1e-6f)
    assertEquals(2f, placed[7], 1e-6f)
  }

  @Test
  fun `a node whose parent is not live, or that has none, is placed by itself`() {
    val orphan = node(5, 99) { x = 3f }
    val top = node(6, null) { y = 4f }

    world.update(1f)

    assertEquals(3f, worldOf(orphan)[0])
    assertEquals(4f, worldOf(top)[1])
  }

  @Test
  fun `moving a parent moves its child on the next tick`() {
    val parent = node(1, null) { x = 1f }
    val child = node(2, 1) { x = 1f }
    world.update(1f)

    with(world) { parent[LocalTransform].x = 5f }
    world.update(1f)

    assertEquals(6f, worldOf(child)[0])
  }

  @Test
  fun `a steady tick allocates nothing`() {
    for (id in 1L..20L) node(id, if (id == 1L) null else id - 1) { x = 1f }
    world.update(1f)

    AllocationBudget.assertNoAllocation { world.update(1f) }
  }
}
