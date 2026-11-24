package dev.chieppa.ease.ecs

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

internal class Position(var x: Float = 0f, var y: Float = 0f) : Component<Position> {
  override fun type() = Position

  companion object : ComponentType<Position>()
}
