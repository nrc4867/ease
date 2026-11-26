package dev.chieppa.ease.physics

import com.github.quillraven.fleks.Entity
import com.github.stephengold.joltjni.BodyCreationSettings
import com.github.stephengold.joltjni.Jolt
import com.github.stephengold.joltjni.enumerate.EActivation
import com.github.stephengold.joltjni.enumerate.EMotionType
import com.github.stephengold.joltjni.readonly.ConstShape
import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.physics.jolt.BodyPoses
import dev.chieppa.ease.physics.jolt.BodyUserData

/**
 * The Jolt body of each entity with a collider, by entity id, and how it moves: static and
 * kinematic bodies follow their entity's transform; a dynamic body is simulated, writing its
 * transform, while its world has authority over it, and otherwise follows its transform as a
 * kinematic body. Nothing here allocates once a body exists.
 */
internal class PhysicsBodies(private val physics: PhysicsWorld) : AutoCloseable {
  private var bodyOf = IntArray(INITIAL_ENTITIES) { NO_BODY }
  private var motionOf = IntArray(INITIAL_ENTITIES)
  private var simulated = BooleanArray(INITIAL_ENTITIES)
  private val poses = BodyPoses(INITIAL_ENTITIES)

  /** The Jolt body id of [entity]'s collider, or [NO_BODY]. */
  fun bodyOf(entity: Entity): Int = bodyOf.getOrElse(entity.id) { NO_BODY }

  /** How [entity]'s body moves: [STATIC], [KINEMATIC] or [DYNAMIC]. */
  fun motionOf(entity: Entity): Int = motionOf[entity.id]

  /** Whether Jolt simulates [entity]'s dynamic body, rather than moving it kinematically. */
  fun isSimulated(entity: Entity): Boolean = simulated.getOrElse(entity.id) { false }

  /**
   * Makes [entity]'s dynamic body simulated when [authority], and kinematic otherwise, as the
   * world's authority over it changes.
   */
  fun simulate(entity: Entity, authority: Boolean) {
    if (simulated[entity.id] == authority) return
    val type = if (authority) EMotionType.Dynamic else EMotionType.Kinematic
    physics.bodyInterface.setMotionType(bodyOf[entity.id], type, EActivation.Activate)
    simulated[entity.id] = authority
  }

  /**
   * Adds a body of [shape] for [entity] at its [transform], moving as [motion], on Jolt object
   * layer [layer] with surface type [surfaceIndex], a sensor when [trigger]. Jolt keeps its own
   * reference to the shape, so the caller closes it. A dynamic body starts kinematic, until
   * [simulate] finds its world has authority over it.
   */
  @Suppress("LongParameterList") // A body's every setting, for both kinds of collider.
  fun add(
    entity: Entity,
    shape: ConstShape,
    transform: LocalTransform,
    motion: Int,
    layer: Int,
    surfaceIndex: Int,
    trigger: Boolean,
  ) {
    remove(entity)
    grow(entity.id)
    val surface = physics.surfaces[surfaceIndex]
    poses.load(transform)
    val id =
      BodyCreationSettings(shape, poses.position, poses.rotation, motionType(motion), layer).use {
        settings ->
        settings.setAllowDynamicOrKinematic(motion == DYNAMIC)
        settings.setUserData(BodyUserData.of(entity.id, surfaceIndex))
        settings.setFriction(surface.friction)
        settings.setRestitution(surface.restitution)
        settings.setIsSensor(trigger)
        val activation = if (motion == STATIC) EActivation.DontActivate else EActivation.Activate
        physics.bodyInterface.createAndAddBody(settings, activation)
      }
    check(id != Jolt.cInvalidBodyId) { "Jolt holds no more bodies (${physics.settings.maxBodies})" }
    bodyOf[entity.id] = id
    motionOf[entity.id] = motion
    simulated[entity.id] = false
    poses.remember(entity.id, transform)
  }

  /** Removes [entity]'s body, if it has one. */
  fun remove(entity: Entity) {
    val id = bodyOf(entity)
    if (id == NO_BODY) return
    physics.bodyInterface.removeBody(id)
    physics.bodyInterface.destroyBody(id)
    bodyOf[entity.id] = NO_BODY
    motionOf[entity.id] = STATIC
    simulated[entity.id] = false
  }

  /**
   * Moves [entity]'s body, unless simulated, to [transform]: a kinematic body over [seconds],
   * pushing what it meets; a static one at once, and only when the transform changed.
   */
  fun follow(entity: Entity, transform: LocalTransform, seconds: Float) {
    val id = bodyOf(entity)
    if (id == NO_BODY) return
    val motion = motionOf[entity.id]
    if (motion == STATIC && !poses.changed(entity.id, transform)) return
    poses.load(transform)
    val position = poses.position
    val rotation = poses.rotation
    if (motion != STATIC) physics.bodyInterface.moveKinematic(id, position, rotation, seconds)
    else
      physics.bodyInterface.setPositionAndRotation(id, position, rotation, EActivation.DontActivate)
    poses.remember(entity.id, transform)
  }

  /** Reads [entity]'s body's pose into [into]'s position and rotation, leaving its scale. */
  fun readPose(entity: Entity, into: LocalTransform) {
    physics.bodyInterface.getPositionAndRotation(bodyOf(entity), poses.position, poses.rotation)
    poses.store(into)
  }

  override fun close() {
    for (id in bodyOf.indices) if (bodyOf[id] != NO_BODY) remove(Entity(id))
  }

  private fun grow(id: Int) {
    if (id < bodyOf.size) return
    val size = maxOf(id + 1, bodyOf.size * 2)
    val old = bodyOf.size
    bodyOf = bodyOf.copyOf(size).also { it.fill(NO_BODY, old, size) }
    motionOf = motionOf.copyOf(size)
    simulated = simulated.copyOf(size)
    poses.grow(size)
  }

  private fun motionType(motion: Int): EMotionType =
    when (motion) {
      KINEMATIC,
      DYNAMIC -> EMotionType.Kinematic
      else -> EMotionType.Static
    }

  companion object {
    const val NO_BODY: Int = Jolt.cInvalidBodyId
    const val STATIC: Int = 0
    const val KINEMATIC: Int = 1
    const val DYNAMIC: Int = 2
    private const val INITIAL_ENTITIES = 64
  }
}
