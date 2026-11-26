package dev.chieppa.ease.animation

import com.github.quillraven.fleks.Entity
import dev.chieppa.ease.animation.TestRig.WALK_SPEED
import dev.chieppa.ease.animation.assets.AnimatorAsset
import dev.chieppa.ease.animation.assets.AnimatorState
import dev.chieppa.ease.animation.events.AnimationListener
import dev.chieppa.ease.animation.graph.AnimatorGraph
import dev.chieppa.ease.animation.scene.Animator
import dev.chieppa.ease.animation.scene.SocketFollower
import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.scene.node.LocalId
import dev.chieppa.ease.scene.node.NodeKey
import dev.chieppa.ease.scene.slots.NodeRef
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * What an animator answers for and when: only nodes with an animator that plays, only while it
 * plays, events once per pass however long a tick, and no backwards states.
 */
class AnimatorLifecycleTest {
  private val test = AnimatedWorld()
  private val rig = TestRig.place(test.world)
  private val heard = mutableListOf<String>()

  @AfterTest fun close() = test.close()

  private val recorder =
    object : AnimationListener {
      override fun clipEvent(animator: Entity, event: String) {
        heard += event
      }

      override fun probe(
        animator: Entity,
        probe: String,
        surface: Int,
        x: Float,
        y: Float,
        z: Float,
      ) = Unit
    }

  private fun walk(ticks: Int) {
    repeat(ticks) {
      test.animators.setParameter(rig.animator, "speed", WALK_SPEED)
      test.step()
    }
  }

  @Test
  fun `setting a parameter on a node without an animator says so instead of failing`() {
    val plain = test.world.entity { it += NodeKey.of(LocalId(PLAIN)) }
    val keyless = test.world.entity { it += Animator() }

    assertFalse(test.animators.setParameter(plain, "speed", 1f))
    assertFalse(test.animators.setParameter(keyless, "speed", 1f))
    test.step()
    assertFalse(test.animators.setParameter(plain, "speed", 1f))
    assertTrue(test.animators.setParameter(rig.animator, "speed", 1f))
  }

  @Test
  fun `an animator removed or emptied stops answering and its sockets stop placing nodes`() {
    val follower =
      test.world.entity {
        it += LocalTransform().apply { y = FOLLOWER_Y }
        it += SocketFollower(NodeRef(NodeKey.of(LocalId(1))), "hand")
      }
    walk(TICKS)
    assertEquals("move", test.animators.stateOf(rig.animator))

    with(test.world) { rig.animator.configure { it -= Animator } }
    test.step()

    assertNull(test.animators.stateOf(rig.animator))
    assertTrue(test.animators.parameter(rig.animator, "speed").isNaN())
    assertEquals(0f, test.animators.clipWeight(rig.animator, TestRig.WALK))
    assertEquals(0f, test.worldOf(follower)[0])
    assertEquals(FOLLOWER_Y, test.worldOf(follower)[1])

    with(test.world) { rig.animator.configure { it += Animator(null) } }
    test.step()
    assertNull(test.animators.stateOf(rig.animator))
  }

  @Test
  fun `a tick a whole walk cycle long fires each footstep once, two cycles twice`() {
    test.animators.events.add(recorder)
    walk(TICKS)
    heard.clear()

    test.world.update(1f)
    assertEquals(listOf("footstepLeft", "footstepRight"), heard.sorted())

    heard.clear()
    test.world.update(2f)
    assertEquals(2, heard.count { it == "footstepLeft" })
    assertEquals(2, heard.count { it == "footstepRight" })
  }

  @Test
  fun `a state that would play backwards does not compile`() {
    val asset = AnimatorAsset(states = listOf(AnimatorState("back", TestRig.IDLE, speed = -1f)))

    val failure =
      assertFailsWith<IllegalArgumentException> { AnimatorGraph.compile(asset) { TestRig.idle } }
    assertTrue("backwards" in failure.message.orEmpty())
  }

  private companion object {
    const val TICKS = 30
    const val PLAIN = 77L
    const val FOLLOWER_Y = 0.1f
  }
}
