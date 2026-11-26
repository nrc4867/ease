package dev.chieppa.ease.sdf.gpu

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World
import com.github.quillraven.fleks.World.Companion.family
import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.ecs.LocalTransformReplicated
import dev.chieppa.ease.ecs.Phase
import dev.chieppa.ease.ecs.phasedWorld
import dev.chieppa.ease.ecs.replication.PeerId
import dev.chieppa.ease.ecs.replication.ReplicationSettings
import dev.chieppa.ease.physics.Physics
import dev.chieppa.ease.physics.character.CharacterController
import dev.chieppa.ease.physics.character.CharacterMotion
import dev.chieppa.ease.physics.character.CharacterMotionReplicated
import dev.chieppa.ease.physics.character.CharacterMover
import dev.chieppa.ease.sdf.mesh.SdfMesh

/** A headless world holding [cave]'s collider and characters that walk at their [Walk]. */
class CaveWorld(cave: SdfMesh) : AutoCloseable {
  val world: World =
    phasedWorld(
      replication =
        ReplicationSettings(
          PeerId.HOST,
          listOf(LocalTransformReplicated, CharacterMotionReplicated),
          true,
        )
    ) {
      Physics.addTo(this)
      predicted { add(Phase.SIMULATION, WalkSystem()) }
    }

  init {
    world.entity {
      it += LocalTransform()
      it += cave.collider()
    }
  }

  /** A character standing at ([x], [y], [z]). */
  fun character(x: Float, y: Float, z: Float): Entity = world.entity {
    it +=
      LocalTransform().apply {
        this.x = x
        this.y = y
        this.z = z
      }
    it += CharacterController()
    it += CharacterMotion()
    it += Walk()
  }

  fun walk(character: Entity, x: Float, z: Float) {
    with(world) {
      character[Walk].x = x
      character[Walk].z = z
    }
  }

  fun step(ticks: Int = 1) = repeat(ticks) { world.update(STEP) }

  fun transformOf(character: Entity): LocalTransform = with(world) { character[LocalTransform] }

  override fun close() = world.dispose()

  /** The velocity a test asks a character to walk at. */
  class Walk(var x: Float = 0f, var z: Float = 0f) : Component<Walk> {
    override fun type(): ComponentType<Walk> = Walk

    companion object : ComponentType<Walk>()
  }

  /** Walks each character at its [Walk] through a [CharacterMover]. */
  class WalkSystem : IteratingSystem(family { all(Walk) }) {
    private val mover = CharacterMover(world)

    override fun onTickEntity(entity: Entity) {
      val walk = entity[Walk]
      mover.move(entity, walk.x, 0f, walk.z, deltaTime)
    }
  }

  companion object {
    const val STEP = 1f / 60f
  }
}
