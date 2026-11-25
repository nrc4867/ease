package dev.chieppa.ease.physics.character

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import dev.chieppa.ease.annotations.Doc
import dev.chieppa.ease.ecs.replication.Replicated
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Doc(
  "A character's moving state, which with its LocalTransform is all the CharacterMover reads, " +
    "so restoring both restores the character exactly, as a client replaying its input does."
)
@Replicated
@Serializable
@SerialName("ease.CharacterMotion")
public class CharacterMotion : Component<CharacterMotion> {
  @Doc("Velocity along x after the last move, in world units a second.")
  public var velocityX: Float = 0f

  @Doc("Velocity along y, up, after the last move, in world units a second.")
  public var velocityY: Float = 0f

  @Doc("Velocity along z after the last move, in world units a second.")
  public var velocityZ: Float = 0f

  @Doc("How far it has walked on the ground since its last footstep, in world units.")
  public var stride: Float = 0f

  @Doc("The surface type it stands on, by index, or -1 in the air.")
  public var groundSurface: Int = NO_SURFACE

  override fun type(): ComponentType<CharacterMotion> = CharacterMotion

  /** The component type. */
  public companion object : ComponentType<CharacterMotion>() {
    /** The ground surface of a character in the air. */
    public const val NO_SURFACE: Int = -1
  }
}
