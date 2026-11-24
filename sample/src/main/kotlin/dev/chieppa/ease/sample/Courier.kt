package dev.chieppa.ease.sample

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Marks the square the [CourierScript] steers. */
@Serializable
@SerialName("sample.Courier")
public class Courier : Component<Courier> {
  override fun type(): ComponentType<Courier> = Courier

  /** The component type. */
  public companion object : ComponentType<Courier>()
}
