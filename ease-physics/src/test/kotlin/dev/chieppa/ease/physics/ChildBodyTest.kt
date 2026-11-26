package dev.chieppa.ease.physics

import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.ecs.Phase
import dev.chieppa.ease.ecs.replication.replication
import dev.chieppa.ease.physics.body.Collider
import dev.chieppa.ease.physics.body.RigidBody
import dev.chieppa.ease.physics.body.ShapeKind
import dev.chieppa.ease.physics.query.RayHit
import dev.chieppa.ease.scene.node.LocalId
import dev.chieppa.ease.scene.node.NodeKey
import dev.chieppa.ease.scene.node.OrderKey
import dev.chieppa.ease.scene.node.Parent
import dev.chieppa.ease.scene.runtime.SceneNodes
import dev.chieppa.ease.scene.transform.WorldTransforms
import dev.chieppa.ease.scene.types.SceneTypes
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** A collider on a node under a parent is placed by the parent, and follows it when it moves. */
class ChildBodyTest {
  private val test = TestWorld {
    it.add(Phase.INPUT, SceneNodes(SceneTypes.build()))
    it.add(Phase.EXTRACT, WorldTransforms())
  }
  private val hit = RayHit()

  @AfterTest fun close() = test.close()

  private fun topBelow(x: Float): Float {
    assertTrue(test.physics.raycast(x, HIGH, 0f, 0f, -1f, 0f, HIGH * 2, hit), "nothing below $x")
    return hit.y
  }

  @Test
  fun `a child's collider sits where its parent places it`() {
    val parent =
      test.world.entity {
        it += NodeKey.of(LocalId(1))
        it +=
          LocalTransform().apply {
            x = 4f
            y = 2f
          }
      }
    test.world.entity {
      it += NodeKey.of(LocalId(2))
      it += Parent(NodeKey.of(LocalId(1)), OrderKey("x"))
      it += LocalTransform().apply { y = 1f }
      it +=
        Collider().apply {
          shape = ShapeKind.BOX
          halfX = 0.5f
          halfY = 0.5f
          halfZ = 0.5f
        }
    }
    test.step()
    assertEquals(3.5f, topBelow(4f), TOLERANCE)

    test.world.replication.write(parent, LocalTransform) { y = 5f }
    test.step()

    assertEquals(6.5f, topBelow(4f), TOLERANCE)
  }

  @Test
  fun `a dynamic body under a parent gets no body and is listed until it leaves the parent`() {
    test.world.entity {
      it += NodeKey.of(LocalId(1))
      it += LocalTransform()
    }
    val child =
      test.world.entity {
        it += NodeKey.of(LocalId(2))
        it += Parent(NodeKey.of(LocalId(1)), OrderKey("x"))
        it += LocalTransform()
        it += RigidBody()
        it += Collider().apply { shape = ShapeKind.BOX }
      }
    test.step()
    val bodies = test.physics.bodyCount
    val problem = test.physics.problems.single()

    with(test.world) { child.configure { it -= Parent } }
    test.step()

    assertTrue("dynamic RigidBody under a Parent" in problem, problem)
    assertEquals(emptyList(), test.physics.problems.toList())
    assertEquals(bodies + 1, test.physics.bodyCount)
  }

  private companion object {
    const val HIGH = 20f
    const val TOLERANCE = 1e-3f
  }
}
