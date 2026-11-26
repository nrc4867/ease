package dev.chieppa.ease.animation

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import dev.chieppa.ease.animation.TestRig.WALK_SPEED
import dev.chieppa.ease.animation.assets.ParameterDrive
import dev.chieppa.ease.animation.scene.Animator
import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.ecs.Phase
import dev.chieppa.ease.ecs.replication.Authority
import dev.chieppa.ease.ecs.replication.PeerId
import dev.chieppa.ease.ecs.replication.mayWrite
import dev.chieppa.ease.ecs.replication.replication
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * On a client, the character it owns is moved by its predicted systems ahead of the host, and its
 * animator's motion-driven speed comes from that predicted movement: the client's character walks
 * at once, without waiting for the host's state.
 */
class PredictedSpeedTest {
  /** Walks every animated node along +z at walking speed, where this world may write it. */
  private class StepForward : IteratingSystem(family { all(Animator, LocalTransform) }) {
    override fun onTickEntity(entity: Entity) {
      val replication = world.replication
      if (!replication.mayWrite(entity)) return
      replication.write(entity, LocalTransform) { z += WALK_SPEED * deltaTime }
    }
  }

  @Test
  fun `a client's own character walks from its predicted movement`() {
    AnimatedWorld(
        ParameterDrive.MOTION_SPEED,
        peer = CLIENT,
        extra = { predicted { add(Phase.SIMULATION, StepForward()) } },
      )
      .use { client ->
        val rig = TestRig.place(client.world)
        with(client.world) { rig.animator.configure { it += Authority(PeerId.HOST, CLIENT) } }

        client.step(TICKS)

        assertEquals(WALK_SPEED, client.animators.parameter(rig.animator, "speed"), 1e-3f)
        assertEquals("move", client.animators.stateOf(rig.animator))
      }
  }

  private companion object {
    val CLIENT = PeerId(2)
    const val TICKS = 30
  }
}
