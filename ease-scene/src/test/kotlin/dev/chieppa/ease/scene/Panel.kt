package dev.chieppa.ease.scene

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import dev.chieppa.ease.annotations.Doc
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Doc("A test component whose field holds a nested object.")
@Serializable
@SerialName("test.Panel")
class Panel(@Doc("The nested margins.") val margin: Margin = Margin()) : Component<Panel> {
  override fun type(): ComponentType<Panel> = Panel

  companion object : ComponentType<Panel>()
}

@Doc("Two margins.")
@Serializable
@SerialName("test.Margin")
class Margin(
  @Doc("The left margin.") var left: Float = 0f,
  @Doc("The right margin.") var right: Float = 0f,
)
