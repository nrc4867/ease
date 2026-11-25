package dev.chieppa.ease.net.relevancy

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import dev.chieppa.ease.annotations.Doc
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Doc(
  "The team an entity belongs to. Under the team filter, only viewers on the same team receive " +
    "it; an entity without one is left to the other filters."
)
@Serializable
@SerialName("ease.Team")
public class Team(@Doc("The team's number.") public val id: Int) : Component<Team> {
  override fun type(): ComponentType<Team> = Team

  /** The component type, and the team of a viewer on none. */
  public companion object : ComponentType<Team>() {
    /** The team of a viewer on no team, which sees no entity that has one. */
    public const val NONE: Int = -1
  }
}
