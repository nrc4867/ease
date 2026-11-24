package dev.chieppa.ease.ecs

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

internal class Velocity(var x: Float = 0f, var y: Float = 0f) : Component<Velocity> {
  override fun type() = Velocity

  companion object : ComponentType<Velocity>()
}
