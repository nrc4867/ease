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
import dev.chieppa.ease.physics.body.MotionKind
import dev.chieppa.ease.physics.body.RigidBody
import dev.chieppa.ease.physics.volume.TriggerVolume

/**
 * Keeps [physics]'s bodies in step with the entities that have a `Collider` and a `LocalTransform`,
 * and steps the simulation once a tick: a body is added with its entity and removed with it, and
 * built again when its collider's fields change or a `RigidBody` or `TriggerVolume` comes or goes
 * (a trigger that stops being one first sees every sensor inside exit); static and kinematic bodies
 * follow their transforms, then Jolt steps, then each dynamic body this world has authority over
 * writes its transform. A dynamic body this world has no authority over moves as a kinematic one,
 * from the transform its authority sends. It owns [physics] and closes it with the world.
 */
public class PhysicsStepSystem(public val physics: PhysicsWorld) :
  IteratingSystem(family { all(Collider, LocalTransform) }), FamilyOnAdd, FamilyOnRemove {
  private val replication: Replication = world.replication
  private var signatures = IntArray(INITIAL_ENTITIES)
  private var triggers = BooleanArray(INITIAL_ENTITIES)

  override fun onAddEntity(entity: Entity) {
    build(entity, signatureOf(entity))
  }

  override fun onRemoveEntity(entity: Entity) {
    if (triggers[entity.id]) physics.triggers.triggerRemoved(entity)
    triggers[entity.id] = false
    physics.bodies.remove(entity)
  }

  private fun signatureOf(entity: Entity): Int =
    BodySignatures.collider(entity[Collider], entity.getOrNull(RigidBody), entity has TriggerVolume)

  private fun build(entity: Entity, signature: Int) {
    val rigid = entity.getOrNull(RigidBody)
    val motion =
      when {
        rigid == null -> PhysicsBodies.STATIC
        rigid.motion == MotionKind.DYNAMIC -> PhysicsBodies.DYNAMIC
        else -> PhysicsBodies.KINEMATIC
      }
    val trigger = entity has TriggerVolume
    physics.bodies.add(entity, entity[Collider], entity[LocalTransform], motion, trigger)
    if (entity.id >= signatures.size) {
      val size = maxOf(entity.id + 1, signatures.size * 2)
      signatures = signatures.copyOf(size)
      triggers = triggers.copyOf(size)
    }
    signatures[entity.id] = signature
    triggers[entity.id] = trigger
  }

  override fun onTick() {
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
    if (bodies.motionOf(entity) == PhysicsBodies.DYNAMIC) {
      bodies.simulate(entity, replication.hasAuthority(entity))
    }
    if (!bodies.isSimulated(entity)) bodies.follow(entity, entity[LocalTransform], deltaTime)
  }

  override fun onDispose() {
    physics.close()
  }

  private companion object {
    const val INITIAL_ENTITIES = 64
  }
}
