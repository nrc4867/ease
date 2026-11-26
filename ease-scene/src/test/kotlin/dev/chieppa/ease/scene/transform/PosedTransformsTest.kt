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
import kotlin.test.Test
import kotlin.test.assertEquals

/** Nodes placed by what a [PosedTransforms] gives in place of their local transforms. */
class PosedTransformsTest {
  private val world: World = phasedWorld {
    add(Phase.INPUT, SceneNodes(Scenes.types))
    add(Phase.EXTRACT, WorldTransforms(initialCapacity = 2))
  }
  private val transforms = world.system<WorldTransforms>()

  private fun node(id: Long, parent: Long?, x: Float = 0f): Entity = world.entity {
    it += NodeKey.of(LocalId(id))
    it += LocalTransform().apply { this.x = x }
    if (parent != null) it += Parent(NodeKey.of(LocalId(parent)), OrderKey("x"))
  }

  @Test
  fun `a pose read from a node that grows the store is kept`() {
    node(1, null)
    val follower = node(2, 1)
    repeat(WorldTransforms.DEFAULT_CAPACITY) { world.entity() }
    val leader = node(3, 1, x = 5f)
    val read = FloatArray(WORLD_TRANSFORM_FLOATS)
    transforms.posed = PosedTransforms { entity, dst, at ->
      if (entity != follower) {
        PosedTransforms.UNPOSED
      } else {
        transforms.write(leader, read, 0)
        WorldTransforms.writeLocal(world, entity, dst, at)
        dst[at] = read[0] + 1f
        PosedTransforms.WORLD
      }
    }
    world.update(1f)

    val out = FloatArray(WORLD_TRANSFORM_FLOATS)
    transforms.write(follower, out, 0)

    assertEquals(6f, out[0])
  }
}
