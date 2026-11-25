package dev.chieppa.ease.physics

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.FamilyOnAdd
import com.github.quillraven.fleks.FamilyOnRemove
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.physics.body.Collider
import dev.chieppa.ease.physics.body.MeshCollider
import dev.chieppa.ease.physics.jolt.JoltShapes

/**
 * Keeps [physics]'s static mesh bodies in step with the entities that have a `MeshCollider` and a
 * `LocalTransform`: a body is built when the entity gains both and removed with it, and moves when
 * its transform changes. An entity that also has a `Collider` throws, since a body is one or the
 * other. Runs before `PhysicsStepSystem`, so a moved mesh is in place for the tick's step.
 */
internal class MeshColliderSystem(private val physics: PhysicsWorld) :
  IteratingSystem(family { all(MeshCollider, LocalTransform) }), FamilyOnAdd, FamilyOnRemove {
  override fun onAddEntity(entity: Entity) {
    require(entity hasNo Collider) {
      "Entity ${entity.id} has both a Collider and a MeshCollider; give it one"
    }
    val collider = entity[MeshCollider]
    val layer = physics.layers.resolve(collider.layer)
    val surface = physics.surfaces.resolve(collider.surface)
    JoltShapes.mesh(collider.mesh).use { shape ->
      physics.bodies.add(
        entity,
        shape,
        entity[LocalTransform],
        PhysicsBodies.STATIC,
        layer,
        surface,
        false,
      )
    }
  }

  override fun onRemoveEntity(entity: Entity) {
    physics.bodies.remove(entity)
  }

  override fun onTickEntity(entity: Entity) {
    physics.bodies.follow(entity, entity[LocalTransform], deltaTime)
  }
}
