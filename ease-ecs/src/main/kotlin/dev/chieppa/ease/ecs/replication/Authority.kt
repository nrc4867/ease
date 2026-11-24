package dev.chieppa.ease.ecs.replication

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Who may change an entity's replicated state ([holder]) and whose entity it is ([owner]), which
 * decides who receives owner-only fields. Every entity of a phased world carries one; a new entity
 * starts held and owned by the world's own peer.
 */
@Serializable
@SerialName("ease.Authority")
public class Authority(
  public var holder: PeerId = PeerId.HOST,
  public var owner: PeerId = PeerId.HOST,
) : Component<Authority> {
  override fun type(): ComponentType<Authority> = Authority

  /** The component type. */
  public companion object : ComponentType<Authority>()
}
