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
 * `LocalTransform`: a body is built when the entity gains both, built again when its `MeshCollider`
 * is replaced (a collider's mesh, layer and surface never change in place), removed with the
 * entity, and moved when its transform changes. An entity that also has a `Collider` throws, since
 * a body is one or the other. Runs before `PhysicsStepSystem`, so a moved mesh is in place for the
 * tick's step.
 */
internal class MeshColliderSystem(private val physics: PhysicsWorld) :
  IteratingSystem(family { all(MeshCollider, LocalTransform) }), FamilyOnAdd, FamilyOnRemove {
  /** The collider each entity's body was built from, by entity id. */
  private var built = arrayOfNulls<MeshCollider>(INITIAL_ENTITIES)

  override fun onAddEntity(entity: Entity) {
    require(entity hasNo Collider) {
      "Entity ${entity.id} has both a Collider and a MeshCollider; give it one"
    }
    build(entity)
  }

  override fun onRemoveEntity(entity: Entity) {
    physics.bodies.remove(entity)
    built[entity.id] = null
  }

  override fun onTickEntity(entity: Entity) {
    if (entity[MeshCollider] !== built[entity.id]) build(entity)
    physics.bodies.follow(entity, entity[LocalTransform], deltaTime)
  }

  private fun build(entity: Entity) {
    val collider = entity[MeshCollider]
    val layer = physics.layers.resolve(collider.layer)
    val surface = physics.surfaces.resolve(collider.surface)
    JoltShapes.mesh(collider.mesh).use { shape ->
      val transform = entity[LocalTransform]
      physics.bodies.add(entity, shape, transform, PhysicsBodies.STATIC, layer, surface, false)
    }
    if (entity.id >= built.size) built = built.copyOf(maxOf(entity.id + 1, built.size * 2))
    built[entity.id] = collider
  }

  private companion object {
    const val INITIAL_ENTITIES = 64
  }
}
