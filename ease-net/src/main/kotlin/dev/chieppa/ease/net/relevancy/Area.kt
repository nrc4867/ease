package dev.chieppa.ease.net.relevancy

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import dev.chieppa.ease.annotations.Doc
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Doc(
  "The area an entity is in, such as a room or a sublevel. Under the area filter, only viewers " +
    "in the same area receive it; an entity without one is left to the other filters."
)
@Serializable
@SerialName("ease.Area")
public class Area(@Doc("The area's number.") public val id: Int) : Component<Area> {
  override fun type(): ComponentType<Area> = Area

  /** The component type, and the area of a viewer in none. */
  public companion object : ComponentType<Area>() {
    /** The area of a viewer in no area, which sees no entity that has one. */
    public const val NONE: Int = -1
  }
}
