package dev.chieppa.ease.physics

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import dev.chieppa.ease.physics.character.CharacterMover

/** A predicted system walking each character at its [Walk] through a [CharacterMover]. */
class WalkSystem : IteratingSystem(family { all(Walk) }) {
  private val mover = CharacterMover(world)

  override fun onTickEntity(entity: Entity) {
    val walk = entity[Walk]
    mover.move(entity, walk.x, walk.y, walk.z, deltaTime)
  }
}
