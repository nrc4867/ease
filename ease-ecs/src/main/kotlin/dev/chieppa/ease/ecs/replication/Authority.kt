package dev.chieppa.ease.ecs.replication

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import dev.chieppa.ease.annotations.Doc
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Doc(
  "Who may change an entity's replicated state (holder) and whose entity it is (owner), which " +
    "decides who receives owner-only fields. A new entity starts held and owned by the world's " +
    "own peer."
)
@Serializable
@SerialName("ease.Authority")
public class Authority(
  @Doc("The peer that may change the entity's replicated state.")
  public var holder: PeerId = PeerId.HOST,
  @Doc("The peer the entity belongs to; it receives the owner-only fields.")
  public var owner: PeerId = PeerId.HOST,
) : Component<Authority> {
  override fun type(): ComponentType<Authority> = Authority

  /** The component type. */
  public companion object : ComponentType<Authority>()
}
