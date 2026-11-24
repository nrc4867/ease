package dev.chieppa.ease.ecs

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family

internal class MovementSystem : IteratingSystem(family { all(Position, Velocity) }) {
  override fun onTickEntity(entity: Entity) {
    val position = entity[Position]
    val velocity = entity[Velocity]
    position.x += velocity.x
    position.y += velocity.y
  }
}
