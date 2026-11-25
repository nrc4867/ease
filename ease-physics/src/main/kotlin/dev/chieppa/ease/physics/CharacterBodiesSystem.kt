package dev.chieppa.ease.physics

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.FamilyOnAdd
import com.github.quillraven.fleks.FamilyOnRemove
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.physics.character.CharacterController
import dev.chieppa.ease.physics.character.CharacterMotion

/**
 * Gives each entity with a `CharacterController`, a `CharacterMotion` and a `LocalTransform` its
 * Jolt character in [physics], for the `CharacterMover`, and frees it with the entity. It does
 * nothing per tick: characters move only when a mover moves them.
 */
public class CharacterBodiesSystem(private val physics: PhysicsWorld) :
  IteratingSystem(family { all(CharacterController, CharacterMotion, LocalTransform) }),
  FamilyOnAdd,
  FamilyOnRemove {
  override fun onAddEntity(entity: Entity) {
    physics.characters.add(entity, entity[CharacterController], entity[LocalTransform])
  }

  override fun onRemoveEntity(entity: Entity) {
    physics.characters.remove(entity)
  }

  override fun onTick() {
    // Characters move only when a mover moves them.
  }

  override fun onTickEntity(entity: Entity) {
    // Never called: onTick visits no entity.
  }
}
