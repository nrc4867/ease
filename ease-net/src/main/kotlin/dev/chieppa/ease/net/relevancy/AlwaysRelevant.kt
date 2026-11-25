package dev.chieppa.ease.net.relevancy

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import dev.chieppa.ease.annotations.Doc
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Doc(
  "Marks an entity every peer receives, whatever the other relevancy filters say, such as a " +
    "match's score keeper."
)
@Serializable
@SerialName("ease.AlwaysRelevant")
public class AlwaysRelevant : Component<AlwaysRelevant> {
  override fun type(): ComponentType<AlwaysRelevant> = AlwaysRelevant

  /** The component type. */
  public companion object : ComponentType<AlwaysRelevant>()
}
