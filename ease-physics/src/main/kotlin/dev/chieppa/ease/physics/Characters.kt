package dev.chieppa.ease.physics

import com.github.quillraven.fleks.Entity
import com.github.stephengold.joltjni.CharacterVirtual
import com.github.stephengold.joltjni.CharacterVirtualSettings
import com.github.stephengold.joltjni.ExtendedUpdateSettings
import com.github.stephengold.joltjni.Plane
import com.github.stephengold.joltjni.Quat
import com.github.stephengold.joltjni.RVec3
import com.github.stephengold.joltjni.Vec3
import com.github.stephengold.joltjni.readonly.ConstShape
import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.physics.character.CharacterController
import dev.chieppa.ease.physics.jolt.JoltShapes

/**
 * The Jolt virtual character of each entity with a character controller, by entity id, with its
 * capsule (two identical ones, so the mover can make Jolt find its contacts again by swapping
 * them), its collision layer and its step settings.
 */
internal class Characters(private val physics: PhysicsWorld) : AutoCloseable {
  private var virtuals = arrayOfNulls<CharacterVirtual>(INITIAL_ENTITIES)
  private var shapes = arrayOfNulls<ConstShape>(INITIAL_ENTITIES)
  private var twins = arrayOfNulls<ConstShape>(INITIAL_ENTITIES)
  private var steps = arrayOfNulls<ExtendedUpdateSettings>(INITIAL_ENTITIES)
  private var layers = IntArray(INITIAL_ENTITIES)

  /** [entity]'s character, or null when it has none. */
  operator fun get(entity: Entity): CharacterVirtual? = virtuals.getOrNull(entity.id)

  /**
   * The twin of the capsule [entity]'s character has, which it is to have next: the two swap places
   * on every call.
   */
  fun swapShape(entity: Entity): ConstShape {
    val id = entity.id
    val next = checkNotNull(twins[id]) { "Entity $id has no character" }
    twins[id] = shapes[id]
    shapes[id] = next
    return next
  }

  /** How [entity]'s character walks up steps and sticks to the floor. */
  fun stepsOf(entity: Entity): ExtendedUpdateSettings =
    checkNotNull(steps[entity.id]) { "Entity ${entity.id} has no character" }

  /** The collision layer [entity]'s character moves on. */
  fun layerOf(entity: Entity): Int = layers[entity.id]

  /** Adds a character for [entity], shaped by [controller], at [transform]'s position. */
  fun add(entity: Entity, controller: CharacterController, transform: LocalTransform) {
    remove(entity)
    grow(entity.id)
    val shape = JoltShapes.standingCapsule(controller.radius, controller.height)
    val twin = JoltShapes.standingCapsule(controller.radius, controller.height)
    val position = RVec3(transform.x.toDouble(), transform.y.toDouble(), transform.z.toDouble())
    val character =
      CharacterVirtualSettings().use { settings ->
        settings.setShape(shape)
        settings.setMaxSlopeAngle(Math.toRadians(controller.maxSlopeDegrees.toDouble()).toFloat())
        // Contacts below the capsule's lower sphere's center are ground.
        settings.setSupportingVolume(Plane(0f, 1f, 0f, -controller.radius))
        CharacterVirtual(settings, position, Quat(), entity.id.toLong(), physics.system)
      }
    virtuals[entity.id] = character
    shapes[entity.id] = shape
    twins[entity.id] = twin
    steps[entity.id] =
      ExtendedUpdateSettings()
        .setWalkStairsStepUp(Vec3(0f, controller.stepHeight, 0f))
        .setStickToFloorStepDown(Vec3(0f, -controller.stepHeight, 0f))
    layers[entity.id] = physics.layers.resolve(controller.layer)
  }

  /** Removes [entity]'s character, if it has one. */
  fun remove(entity: Entity) {
    val id = entity.id
    val character = virtuals.getOrNull(id) ?: return
    character.close()
    shapes[id]?.close()
    twins[id]?.close()
    steps[id]?.close()
    virtuals[id] = null
    shapes[id] = null
    twins[id] = null
    steps[id] = null
  }

  override fun close() {
    for (id in virtuals.indices) remove(Entity(id))
  }

  private fun grow(id: Int) {
    if (id < virtuals.size) return
    val size = maxOf(id + 1, virtuals.size * 2)
    virtuals = virtuals.copyOf(size)
    shapes = shapes.copyOf(size)
    twins = twins.copyOf(size)
    steps = steps.copyOf(size)
    layers = layers.copyOf(size)
  }

  private companion object {
    const val INITIAL_ENTITIES = 64
  }
}
