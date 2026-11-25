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
 * Jolt character in [physics], for the `CharacterMover`, builds it again when the controller's
 * fields change, and frees it with the entity. Characters move only when a mover moves them.
 */
public class CharacterBodiesSystem(private val physics: PhysicsWorld) :
  IteratingSystem(family { all(CharacterController, CharacterMotion, LocalTransform) }),
  FamilyOnAdd,
  FamilyOnRemove {
  override fun onAddEntity(entity: Entity) {
    val controller = entity[CharacterController]
    physics.characters.add(entity, controller, entity[LocalTransform], signature(controller))
  }

  private fun signature(controller: CharacterController): Int =
    BodySignatures.controller(controller)

  override fun onRemoveEntity(entity: Entity) {
    physics.characters.remove(entity)
  }

  override fun onTickEntity(entity: Entity) {
    val controller = entity[CharacterController]
    val signature = signature(controller)
    if (signature != physics.characters.signatureOf(entity)) {
      physics.characters.add(entity, controller, entity[LocalTransform], signature)
    }
  }
}
