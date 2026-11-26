package dev.chieppa.ease.animation.scene

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import dev.chieppa.ease.annotations.Doc
import dev.chieppa.ease.scene.slots.NodeRef
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Doc(
  "Places a node at a socket of an animated model, such as a sword in a hand slot: its world " +
    "transform is the socket's, with its own LocalTransform as an offset in the socket's space, " +
    "and its Parent is not used for placing it. It follows the pose every peer works out, so it " +
    "needs no replication of its own."
)
@Serializable
@SerialName("ease.SocketFollower")
public class SocketFollower(
  @Doc("The node carrying the Animator whose socket it follows.") public val animator: NodeRef,
  @Doc("The socket's name in that animator asset.") public val socket: String,
) : Component<SocketFollower> {
  override fun type(): ComponentType<SocketFollower> = SocketFollower

  override fun equals(other: Any?): Boolean =
    other is SocketFollower && other.animator == animator && other.socket == socket

  override fun hashCode(): Int = animator.hashCode() * HASH_PRIME + socket.hashCode()

  /** The component type. */
  public companion object : ComponentType<SocketFollower>() {
    private const val HASH_PRIME = 31
  }
}
