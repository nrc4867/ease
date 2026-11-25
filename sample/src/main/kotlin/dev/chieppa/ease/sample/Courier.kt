package dev.chieppa.ease.sample

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import dev.chieppa.ease.annotations.Doc
import dev.chieppa.ease.ecs.replication.Replicated
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Doc("Marks a peer's courier, the square its input moves.")
@Replicated
@Serializable
@SerialName("sample.Courier")
public class Courier : Component<Courier> {
  override fun type(): ComponentType<Courier> = Courier

  /** The component type. */
  public companion object : ComponentType<Courier>()
}
