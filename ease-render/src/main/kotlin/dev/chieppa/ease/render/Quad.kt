package dev.chieppa.ease.render

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import dev.chieppa.ease.annotations.Doc
import dev.chieppa.ease.annotations.Range
import dev.chieppa.ease.ecs.replication.Quantized
import dev.chieppa.ease.ecs.replication.Replicated
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Doc(
  "Draws its entity as a square centered on the entity's LocalTransform position, in an opaque " +
    "color. It replicates with each color channel quantized."
)
@Replicated
@Serializable
@SerialName("ease.Quad")
public class Quad : Component<Quad> {
  @Doc("The square's side, in world units.") public var size: Float = 1f

  @Doc("Red channel, 0 to 1.") @Quantized(CHANNEL_BITS) @Range(0.0, 1.0) public var red: Float = 1f

  @Doc("Green channel, 0 to 1.")
  @Quantized(CHANNEL_BITS)
  @Range(0.0, 1.0)
  public var green: Float = 1f

  @Doc("Blue channel, 0 to 1.")
  @Quantized(CHANNEL_BITS)
  @Range(0.0, 1.0)
  public var blue: Float = 1f

  override fun type(): ComponentType<Quad> = Quad

  /** The component type, and a factory for a quad of a size and color. */
  public companion object : ComponentType<Quad>() {
    /** Bits each color channel takes on the wire. */
    public const val CHANNEL_BITS: Int = 8

    /** A quad [size] units on a side in the given color. */
    public fun of(size: Float, red: Float, green: Float, blue: Float): Quad =
      Quad().apply {
        this.size = size
        this.red = red
        this.green = green
        this.blue = blue
      }
  }
}
