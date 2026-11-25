package dev.chieppa.ease.render.scene

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import dev.chieppa.ease.annotations.Doc
import dev.chieppa.ease.annotations.Range
import dev.chieppa.ease.ecs.replication.Quantized
import dev.chieppa.ease.ecs.replication.Replicated
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Doc(
  "A sun: parallel light shining down its entity's LocalTransform -z. The world is lit by the " +
    "light of the lowest entity id. Its color, scaled by ambient, also reaches every surface " +
    "whatever its direction."
)
@Replicated
@Serializable
@SerialName("ease.DirectionalLight")
public class DirectionalLight : Component<DirectionalLight> {
  @Doc("Light color red, linear, 0 to 1.")
  @Quantized(CHANNEL_BITS)
  @Range(0.0, 1.0)
  public var red: Float = 1f

  @Doc("Light color green, linear, 0 to 1.")
  @Quantized(CHANNEL_BITS)
  @Range(0.0, 1.0)
  public var green: Float = 1f

  @Doc("Light color blue, linear, 0 to 1.")
  @Quantized(CHANNEL_BITS)
  @Range(0.0, 1.0)
  public var blue: Float = 1f

  @Doc("How strong the direct light is.") public var intensity: Float = DEFAULT_INTENSITY

  @Doc("How much of the light's color reaches every surface whatever its direction.")
  public var ambient: Float = DEFAULT_AMBIENT

  override fun type(): ComponentType<DirectionalLight> = DirectionalLight

  /** The component type, and a factory for a light. */
  public companion object : ComponentType<DirectionalLight>() {
    /** Bits each color channel takes on the wire. */
    public const val CHANNEL_BITS: Int = 8

    /** The intensity a light has unless told otherwise. */
    public const val DEFAULT_INTENSITY: Float = 3f

    /** The ambient share a light has unless told otherwise. */
    public const val DEFAULT_AMBIENT: Float = 0.15f

    /** A white light of [intensity] and [ambient]. */
    public fun of(
      intensity: Float = DEFAULT_INTENSITY,
      ambient: Float = DEFAULT_AMBIENT,
    ): DirectionalLight =
      DirectionalLight().also {
        it.intensity = intensity
        it.ambient = ambient
      }
  }
}
