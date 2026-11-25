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
 * and steps the simulation once a tick: a body is added with its entity and removed with it; static
 * and kinematic bodies follow their transforms, then Jolt steps, then each dynamic body this world
 * has authority over writes its transform. A dynamic body this world has no authority over moves as
 * a kinematic one, from the transform its authority sends. It owns [physics] and closes it with the
 * world.
 */
public class PhysicsStepSystem(public val physics: PhysicsWorld) :
  IteratingSystem(family { all(Collider, LocalTransform) }), FamilyOnAdd, FamilyOnRemove {
  private val replication: Replication = world.replication

  override fun onAddEntity(entity: Entity) {
    val rigid = entity.getOrNull(RigidBody)
    val motion =
      when {
        rigid == null -> PhysicsBodies.STATIC
        rigid.motion == MotionKind.DYNAMIC -> PhysicsBodies.DYNAMIC
        else -> PhysicsBodies.KINEMATIC
      }
    physics.bodies.add(
      entity,
      entity[Collider],
      entity[LocalTransform],
      motion,
      entity has TriggerVolume,
    )
  }

  override fun onRemoveEntity(entity: Entity) {
    physics.bodies.remove(entity)
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
    if (bodies.motionOf(entity) == PhysicsBodies.DYNAMIC) {
      bodies.simulate(entity, replication.hasAuthority(entity))
    }
    if (!bodies.isSimulated(entity)) bodies.follow(entity, entity[LocalTransform], deltaTime)
  }

  override fun onDispose() {
    physics.close()
  }
}
