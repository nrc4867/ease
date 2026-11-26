package dev.chieppa.ease.sample

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import dev.chieppa.ease.annotations.Doc
import dev.chieppa.ease.annotations.Range
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Doc(
  "Walks a node back and forth along x between -reach and +reach, speeding up from a stop, " +
    "slowing into the far end, turning round there to face the way back and waiting."
)
@Serializable
@SerialName("sample.Stroller")
public class Stroller : Component<Stroller> {
  @Doc("How far either side of x = 0 the walk reaches, in world units.")
  @Range(0.0, MAX_REACH)
  public var reach: Float = DEFAULT_REACH

  @Doc("The walking speed, in world units a second.")
  @Range(0.0, MAX_SPEED)
  public var speed: Float = DEFAULT_SPEED

  @Doc("How fast it speeds up and slows down, in world units a second squared.")
  @Range(0.0, MAX_SPEED)
  public var acceleration: Float = DEFAULT_ACCELERATION

  @Doc("How long it waits at each end, in seconds.")
  @Range(0.0, MAX_PAUSE)
  public var pause: Float = DEFAULT_PAUSE

  @Doc("Which way it walks: 1 along +x, -1 along -x.") public var direction: Float = 1f

  @Doc("How long it has walked this leg, in seconds.") public var walked: Float = 0f

  @Doc("How much longer it waits before walking, in seconds.") public var waiting: Float = 0f

  override fun type(): ComponentType<Stroller> = Stroller

  /** The component type and its defaults. */
  public companion object : ComponentType<Stroller>() {
    /** How far either side of the middle a walk reaches unless told otherwise. */
    public const val DEFAULT_REACH: Float = 3f

    /** The walking speed unless told otherwise. */
    public const val DEFAULT_SPEED: Float = 0.8f

    /** How fast a stroller speeds up and slows down unless told otherwise. */
    public const val DEFAULT_ACCELERATION: Float = 1.6f

    /** How long a stroller waits at each end unless told otherwise. */
    public const val DEFAULT_PAUSE: Float = 1.5f

    private const val MAX_REACH = 100.0
    private const val MAX_SPEED = 20.0
    private const val MAX_PAUSE = 60.0
  }
}
