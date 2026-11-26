package dev.chieppa.ease.physics

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.FamilyOnAdd
import com.github.quillraven.fleks.FamilyOnRemove
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.ecs.replication.Replication
import dev.chieppa.ease.ecs.replication.replication
import dev.chieppa.ease.physics.body.Collider
import dev.chieppa.ease.physics.body.MeshCollider
import dev.chieppa.ease.physics.body.MotionKind
import dev.chieppa.ease.physics.body.RigidBody
import dev.chieppa.ease.physics.jolt.JoltShapes
import dev.chieppa.ease.physics.volume.TriggerVolume
import dev.chieppa.ease.scene.node.Parent
import dev.chieppa.ease.scene.transform.WORLD_TRANSFORM_FLOATS
import dev.chieppa.ease.scene.transform.WorldTransforms

/**
 * Keeps [physics]'s bodies in step with the entities that have a `Collider` and a `LocalTransform`,
 * and steps the simulation once a tick: a body is added with its entity and removed with it, and
 * built again when its collider's fields change or a `RigidBody` or `TriggerVolume` comes or goes
 * (a trigger that stops being one first sees every sensor inside exit); static and kinematic bodies
 * follow their transforms, then Jolt steps, then each dynamic body this world has authority over
 * writes its transform. Bodies are placed at world transforms: a node under a parent is placed by
 * the world's `WorldTransforms`, worked out afresh each step; a dynamic body writes its pose as its
 * local transform, so one under a parent gets no body and is listed in the physics world's
 * `problems` until its parent or rigid body changes. A dynamic body this world has no authority
 * over moves as a kinematic one, from the transform its authority sends. It owns [physics] and
 * closes it with the world.
 */
public class PhysicsStepSystem(public val physics: PhysicsWorld) :
  IteratingSystem(family { all(Collider, LocalTransform) }), FamilyOnAdd, FamilyOnRemove {
  private val replication: Replication = world.replication
  private var signatures = IntArray(INITIAL_ENTITIES)
  private var triggers = BooleanArray(INITIAL_ENTITIES)
  private var worlds: WorldTransforms? = null
  private val placed = LocalTransform()
  private val pose = FloatArray(WORLD_TRANSFORM_FLOATS)

  override fun onInit() {
    worlds = world.systemOrNull<WorldTransforms>()
  }

  /**
   * Where [entity] is in the world: its local transform, or for a node under a parent its world
   * transform from the world's [WorldTransforms], copied into one reused transform.
   */
  private fun poseOf(entity: Entity): LocalTransform {
    val worlds = worlds
    if (worlds == null || entity hasNo Parent) return entity[LocalTransform]
    worlds.write(entity, pose, 0)
    placed.x = pose[0]
    placed.y = pose[1]
    placed.z = pose[2]
    placed.rotationX = pose[ROTATION_AT]
    placed.rotationY = pose[ROTATION_AT + 1]
    placed.rotationZ = pose[ROTATION_AT + 2]
    placed.rotationW = pose[ROTATION_AT + QUAT_W]
    placed.scaleX = pose[SCALE_AT]
    placed.scaleY = pose[SCALE_AT + 1]
    placed.scaleZ = pose[SCALE_AT + 2]
    return placed
  }

  override fun onAddEntity(entity: Entity) {
    require(entity hasNo MeshCollider) {
      "Entity ${entity.id} has both a Collider and a MeshCollider; give it one"
    }
    build(entity, signatureOf(entity))
  }

  override fun onRemoveEntity(entity: Entity) {
    if (triggers[entity.id]) physics.triggers.triggerRemoved(entity)
    triggers[entity.id] = false
    physics.bodies.remove(entity)
    physics.refused.remove(entity.id)
  }

  /** The collider's signature, its parent's presence folded in, so gaining or losing one builds. */
  private fun signatureOf(entity: Entity): Int {
    val collider =
      BodySignatures.collider(
        entity[Collider],
        entity.getOrNull(RigidBody),
        entity has TriggerVolume,
      )
    return collider * 2 + if (entity has Parent) 1 else 0
  }

  private fun build(entity: Entity, signature: Int) {
    val rigid = entity.getOrNull(RigidBody)
    val motion =
      when {
        rigid == null -> PhysicsBodies.STATIC
        rigid.motion == MotionKind.DYNAMIC -> PhysicsBodies.DYNAMIC
        else -> PhysicsBodies.KINEMATIC
      }
    if (motion == PhysicsBodies.DYNAMIC && entity has Parent) {
      if (entity.id < triggers.size && triggers[entity.id]) physics.triggers.triggerRemoved(entity)
      physics.bodies.remove(entity)
      remember(entity, signature, trigger = false)
      physics.refused[entity.id] =
        "Entity ${entity.id} has a dynamic RigidBody under a Parent, so it has no body: " +
          "a dynamic body writes its pose as its local transform, so it must be at the top " +
          "of the tree"
      return
    }
    if (physics.refused.isNotEmpty()) physics.refused.remove(entity.id)
    val trigger = entity has TriggerVolume
    val collider = entity[Collider]
    val layer = if (trigger) physics.joltLayers.trigger else physics.layers.resolve(collider.layer)
    val surface = physics.surfaces.resolve(collider.surface)
    JoltShapes.of(collider).use { shape ->
      physics.bodies.add(entity, shape, poseOf(entity), motion, layer, surface, trigger)
    }
    remember(entity, signature, trigger)
  }

  private fun remember(entity: Entity, signature: Int, trigger: Boolean) {
    if (entity.id >= signatures.size) {
      val size = maxOf(entity.id + 1, signatures.size * 2)
      signatures = signatures.copyOf(size)
      triggers = triggers.copyOf(size)
    }
    signatures[entity.id] = signature
    triggers[entity.id] = trigger
  }

  override fun onTick() {
    worlds?.invalidate()
    super.onTick()
    physics.step(deltaTime)
    family.forEach { entity ->
      if (physics.bodies.isSimulated(entity)) {
        replication.write(entity, LocalTransform) { physics.bodies.readPose(entity, this) }
      }
    }
  }

  override fun onTickEntity(entity: Entity) {
    val bodies = physics.bodies
    val signature = signatureOf(entity)
    if (signature != signatures[entity.id]) {
      if (triggers[entity.id] && entity hasNo TriggerVolume) {
        physics.triggers.triggerRemoved(entity)
      }
      build(entity, signature)
    }
    if (bodies.bodyOf(entity) == PhysicsBodies.NO_BODY) return
    if (bodies.motionOf(entity) == PhysicsBodies.DYNAMIC) {
      bodies.simulate(entity, replication.hasAuthority(entity))
    }
    if (!bodies.isSimulated(entity)) bodies.follow(entity, poseOf(entity), deltaTime)
  }

  override fun onDispose() {
    physics.close()
  }

  private companion object {
    const val INITIAL_ENTITIES = 64
    const val ROTATION_AT = 3
    const val SCALE_AT = 7
    const val QUAT_W = 3
  }
}
