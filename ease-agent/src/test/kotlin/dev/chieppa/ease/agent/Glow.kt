package dev.chieppa.ease.agent

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** How brightly an entity glows; the test game's own component. */
@Serializable
@SerialName("test.Glow")
internal class Glow : Component<Glow> {
  var level: Float = 0f

  override fun type(): ComponentType<Glow> = Glow

  companion object : ComponentType<Glow>()
}
