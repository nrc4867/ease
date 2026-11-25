package dev.chieppa.ease.physics

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.ecs.LocalTransformReplicated
import dev.chieppa.ease.ecs.Phase
import dev.chieppa.ease.ecs.phasedWorld
import dev.chieppa.ease.ecs.replication.Authority
import dev.chieppa.ease.ecs.replication.PeerId
import dev.chieppa.ease.ecs.replication.ReplicationSettings
import dev.chieppa.ease.math.Quat
import dev.chieppa.ease.math.Transform
import dev.chieppa.ease.math.Vec3
import dev.chieppa.ease.physics.body.Collider
import dev.chieppa.ease.physics.body.ShapeKind
import dev.chieppa.ease.physics.character.CharacterController
import dev.chieppa.ease.physics.character.CharacterMotion
import dev.chieppa.ease.physics.character.CharacterMotionReplicated
import dev.chieppa.ease.physics.setup.PhysicsSettings
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * A headless world with physics and a predicted [WalkSystem], run as [peer], with helpers to place
 * boxes and characters. The floor's top is at y = 0.
 */
class TestWorld(
  settings: PhysicsSettings = PhysicsSettings(),
  val peer: PeerId = PeerId.HOST,
  debugChecks: Boolean = true,
  extra: (dev.chieppa.ease.ecs.PhasedSystems) -> Unit = {},
) : AutoCloseable {
  val world: World =
    phasedWorld(
      replication =
        ReplicationSettings(
          peer,
          listOf(LocalTransformReplicated, CharacterMotionReplicated),
          debugChecks,
        )
    ) {
      Physics.addTo(this, settings)
      extra(this)
      predicted { add(Phase.SIMULATION, WalkSystem()) }
    }

  val physics: PhysicsWorld
    get() = world.physics

  /** A static box centered at ([x], [y], [z]), turned [degreesAboutZ] about z. */
  @Suppress("LongParameterList")
  fun box(
    x: Float,
    y: Float,
    z: Float,
    halfX: Float,
    halfY: Float,
    halfZ: Float,
    layer: String = "",
    surface: String = "",
    degreesAboutZ: Float = 0f,
    configure: (Collider) -> Unit = {},
  ): Entity = world.entity {
    it += placed(x, y, z, degreesAboutZ)
    it +=
      Collider().apply {
        shape = ShapeKind.BOX
        this.halfX = halfX
        this.halfY = halfY
        this.halfZ = halfZ
        this.layer = layer
        this.surface = surface
        configure(this)
      }
  }

  /** The floor, 100 units square, its top at y = 0. */
  fun floor(surface: String = ""): Entity =
    box(0f, -HALF, 0f, FLOOR, HALF, FLOOR, surface = surface)

  /** A character standing at ([x], [y], [z]) on [layer], owned by [owner]. */
  @Suppress("LongParameterList")
  fun character(
    x: Float,
    y: Float = 0f,
    z: Float = 0f,
    layer: String = "",
    owner: PeerId = peer,
    configure: (World, Entity) -> Unit = { _, _ -> },
  ): Entity =
    world
      .entity {
        it += placed(x, y, z, 0f)
        it += CharacterController().apply { this.layer = layer }
        it += CharacterMotion()
        it += Walk()
        if (owner != peer) it += Authority(PeerId.HOST, owner)
      }
      .also { configure(world, it) }

  /** Runs [ticks] fixed steps. */
  fun step(ticks: Int = 1) = repeat(ticks) { world.update(STEP) }

  fun transformOf(entity: Entity): LocalTransform = with(world) { entity[LocalTransform] }

  fun motionOf(entity: Entity): CharacterMotion = with(world) { entity[CharacterMotion] }

  fun walk(entity: Entity, x: Float, z: Float = 0f, y: Float = 0f) {
    with(world) { entity[Walk] }
      .let {
        it.x = x
        it.y = y
        it.z = z
      }
  }

  override fun close() = world.dispose()

  companion object {
    const val STEP = 1f / 60f
    private const val HALF = 0.5f
    private const val FLOOR = 50f

    fun placed(x: Float, y: Float, z: Float, degreesAboutZ: Float): LocalTransform {
      val half = degreesAboutZ * PI.toFloat() / 360f
      return LocalTransform.of(
        Transform(Vec3(x, y, z), Quat(0f, 0f, sin(half), cos(half)), Vec3(1f, 1f, 1f))
      )
    }
  }
}
