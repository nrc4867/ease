package dev.chieppa.ease.physics

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

/** The velocity a test asks a character to walk at, as its input would. */
class Walk(var x: Float = 0f, var y: Float = 0f, var z: Float = 0f) : Component<Walk> {
  override fun type(): ComponentType<Walk> = Walk

  companion object : ComponentType<Walk>()
}
