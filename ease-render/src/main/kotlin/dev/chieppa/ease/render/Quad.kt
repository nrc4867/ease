package dev.chieppa.ease.render

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import dev.chieppa.ease.ecs.replication.Quantized
import dev.chieppa.ease.ecs.replication.Replicated
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Draws its entity as a square of [size] world units on a side, centered on the entity's
 * `LocalTransform` position, in an opaque color with channels from 0 to 1. It replicates with each
 * channel quantized to 8 bits.
 */
@Replicated
@Serializable
@SerialName("ease.Quad")
public class Quad : Component<Quad> {
  public var size: Float = 1f
  @Quantized(CHANNEL_BITS, 0f, 1f) public var red: Float = 1f
  @Quantized(CHANNEL_BITS, 0f, 1f) public var green: Float = 1f
  @Quantized(CHANNEL_BITS, 0f, 1f) public var blue: Float = 1f

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
