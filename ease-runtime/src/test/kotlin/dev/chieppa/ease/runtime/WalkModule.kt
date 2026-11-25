package dev.chieppa.ease.runtime

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import dev.chieppa.ease.commands.builtin.Spawn
import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.ecs.Phase
import dev.chieppa.ease.ecs.PhasedSystems
import dev.chieppa.ease.ecs.motion.KinematicMover
import dev.chieppa.ease.ecs.replication.Authority
import dev.chieppa.ease.ecs.replication.PeerId
import dev.chieppa.ease.ecs.replication.replication
import dev.chieppa.ease.net.input.peerInputs
import dev.chieppa.ease.render.Quad

/**
 * A game for prediction: each peer that joins gets a walker it owns, which its input's first axis
 * walks along x at [STEP] a tick, a predicted system; the host's drifter moves [STEP] a tick along
 * x by a host system; and while [gusts] is on, a host system pushes every walker [GUST] along y
 * every [GUST_EVERY] ticks, which no client can predict.
 */
internal class WalkModule(private val gusts: () -> Boolean = { false }) : GameModule {
  override fun populate(world: HostedWorld) {
    world.execute(Spawn(listOf<Component<out Any>>(LocalTransform(), Quad.of(1f, 1f, 1f, 1f))))
  }

  override fun peerJoined(world: HostedWorld, peer: PeerId) {
    val walker =
      listOf<Component<out Any>>(
        LocalTransform(),
        Quad.of(1f, 1f, 1f, 1f),
        Authority(PeerId.HOST, peer),
      )
    world.execute(Spawn(walker))
  }

  override fun predictedSystems(systems: PhasedSystems) {
    systems.add(Phase.SIMULATION, Walk())
  }

  override fun hostSystems(systems: PhasedSystems) {
    systems.add(Phase.SIMULATION, Drift())
    systems.add(Phase.PHYSICS, Gust(gusts))
  }

  private class Walk : IteratingSystem(family { all(LocalTransform, Authority) }) {
    private val inputs = world.peerInputs
    private val mover = KinematicMover(world)

    override fun onTickEntity(entity: Entity) {
      val owner = entity[Authority].owner
      if (owner == PeerId.HOST) return
      val input = inputs.of(owner) ?: return
      mover.move(entity, input.axis(0) * STEP / deltaTime, 0f, 0f, deltaTime)
    }
  }

  private class Drift : IteratingSystem(family { all(LocalTransform, Authority) }) {
    private val replication = world.replication

    override fun onTickEntity(entity: Entity) {
      if (entity[Authority].owner == PeerId.HOST) {
        replication.write(entity, LocalTransform) { x += STEP }
      }
    }
  }

  private class Gust(private val on: () -> Boolean) :
    IteratingSystem(family { all(LocalTransform, Authority) }) {
    private val replication = world.replication
    private var ticks = 0L

    override fun onTick() {
      ticks++
      if (on() && ticks % GUST_EVERY == 0L) super.onTick()
    }

    override fun onTickEntity(entity: Entity) {
      if (entity[Authority].owner != PeerId.HOST) {
        replication.write(entity, LocalTransform) { y += GUST }
      }
    }
  }

  companion object {
    /** How far a walker at full input, and the drifter, move each tick. */
    const val STEP = 1f

    /** How far a gust pushes a walker. */
    const val GUST = 0.5f

    /** Ticks between gusts. */
    const val GUST_EVERY = 50L
  }
}
