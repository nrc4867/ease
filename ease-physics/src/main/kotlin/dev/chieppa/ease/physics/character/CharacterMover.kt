package dev.chieppa.ease.physics.character

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import com.github.stephengold.joltjni.CharacterVirtual
import com.github.stephengold.joltjni.RVec3
import com.github.stephengold.joltjni.Vec3
import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.ecs.motion.Mover
import dev.chieppa.ease.ecs.replication.Replication
import dev.chieppa.ease.ecs.replication.mayWrite
import dev.chieppa.ease.ecs.replication.replication
import dev.chieppa.ease.physics.PhysicsWorld
import dev.chieppa.ease.physics.jolt.BodyUserData
import dev.chieppa.ease.physics.physics
import kotlin.math.sqrt

/**
 * The [Mover] that walks a character against the world's colliders, on Jolt's virtual character: it
 * walks up slopes up to its controller's steepest and steps up to its tallest, slides along walls,
 * falls under gravity and is carried by what it stands on. The velocity asked for is horizontal
 * along x and z; a positive y while on the ground is a jump at that speed.
 *
 * Every move starts from the entity's `LocalTransform` and `CharacterMotion` alone: Jolt's
 * character is placed there and its contacts found again before it moves, so restoring those two
 * components and moving again with the same input gives the same result, bit for bit, as a client
 * replaying its input needs. Each move writes both, reports a footstep every stride walked on the
 * ground and a change of the ground's surface type through the world's physics events. Make it
 * where [world] is current, as inside a system. Nothing here allocates.
 */
public class CharacterMover(private val world: World) : Mover {
  private val replication: Replication = world.replication
  private var resolved: PhysicsWorld? = null
  private val position = RVec3()
  private val velocity = Vec3()
  private val ground = Vec3()
  private val gravity = Vec3()

  override fun move(
    entity: Entity,
    velocityX: Float,
    velocityY: Float,
    velocityZ: Float,
    seconds: Float,
  ): Boolean {
    val physics = resolved ?: world.physics.also { resolved = it }
    val character = physics.characters[entity]
    if (character == null || !replication.mayWrite(entity)) return false
    val transform = with(world) { entity[LocalTransform] }
    val motion = with(world) { entity[CharacterMotion] }
    place(character, physics, entity, transform, motion)
    walk(character, physics, entity, motion, velocityX, velocityY, velocityZ, seconds)
    character.getPosition(position)
    character.getLinearVelocity(velocity)
    val surface =
      if (character.isSupported) BodyUserData.surface(character.groundUserData)
      else CharacterMotion.NO_SURFACE
    val walked = if (surface == CharacterMotion.NO_SURFACE) 0f else walked(transform)
    write(entity, physics, motion, surface, walked)
    return true
  }

  /** Puts [character] where the components say and finds its contacts and ground there. */
  private fun place(
    character: CharacterVirtual,
    physics: PhysicsWorld,
    entity: Entity,
    transform: LocalTransform,
    motion: CharacterMotion,
  ) {
    position.set(transform.x.toDouble(), transform.y.toDouble(), transform.z.toDouble())
    character.setPosition(position)
    character.setLinearVelocity(motion.velocityX, motion.velocityY, motion.velocityZ)
    val layer = physics.characters.layerOf(entity)
    val filters = physics.filters
    // Jolt finds contacts again only for a new shape, so each move swaps in the capsule's twin.
    character.setShape(
      physics.characters.swapShape(entity),
      Float.MAX_VALUE / 2,
      filters.broadPhaseOf[layer],
      filters.objectsOf[layer],
      filters.anyBody,
      filters.anyShape,
      physics.temp,
    )
  }

  @Suppress("LongParameterList") // The asked velocity is three numbers; boxing them allocates.
  private fun walk(
    character: CharacterVirtual,
    physics: PhysicsWorld,
    entity: Entity,
    motion: CharacterMotion,
    askedX: Float,
    askedY: Float,
    askedZ: Float,
    seconds: Float,
  ) {
    character.getGroundNormal(ground)
    val onGround = character.isSupported && !character.isSlopeTooSteep(ground.x, ground.y, ground.z)
    character.getGroundVelocity(ground)
    var x = 0f
    var y = motion.velocityY
    var z = 0f
    if (onGround && motion.velocityY - ground.y < LANDING_SPEED) {
      x = ground.x
      y = ground.y + maxOf(askedY, 0f)
      z = ground.z
    }
    val gravityY = physics.settings.gravityY
    character.setLinearVelocity(x + askedX, y + gravityY * seconds, z + askedZ)
    gravity.set(0f, gravityY, 0f)
    val layer = physics.characters.layerOf(entity)
    val filters = physics.filters
    character.extendedUpdate(
      seconds,
      gravity,
      physics.characters.stepsOf(entity),
      filters.broadPhaseOf[layer],
      filters.objectsOf[layer],
      filters.anyBody,
      filters.anyShape,
      physics.temp,
    )
  }

  /** How far the character moved across the ground from [from] to where it is now. */
  private fun walked(from: LocalTransform): Float {
    val dx = position.x() - from.x
    val dz = position.z() - from.z
    return sqrt(dx * dx + dz * dz)
  }

  private fun write(
    entity: Entity,
    physics: PhysicsWorld,
    motion: CharacterMotion,
    surface: Int,
    walked: Float,
  ) {
    val stride = with(world) { entity[CharacterController] }.strideLength
    val lastSurface = motion.groundSurface
    var strides = motion.stride + walked
    val stepped = strides >= stride
    if (stepped) strides %= stride
    replication.write(entity, LocalTransform) {
      x = position.x()
      y = position.y()
      z = position.z()
    }
    replication.write(entity, CharacterMotion) {
      velocityX = velocity.x
      velocityY = velocity.y
      velocityZ = velocity.z
      this.stride = strides
      groundSurface = surface
    }
    if (surface != lastSurface) physics.events.groundSurfaceChanged(entity, surface)
    if (stepped) physics.events.footstep(entity, surface)
  }

  private companion object {
    /** Rising faster than the ground by this much, in world units a second, is leaving it. */
    const val LANDING_SPEED = 0.1f
  }
}
