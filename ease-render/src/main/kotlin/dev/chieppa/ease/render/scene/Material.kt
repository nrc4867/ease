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
  "The lit material its entity's mesh is drawn with: a linear base color, roughness and " +
    "metallic, and emitted light. Colors and factors replicate quantized."
)
@Replicated
@Serializable
@SerialName("ease.Material")
public class Material : Component<Material> {
  @Doc("Base color red, linear, 0 to 1.")
  @Quantized(CHANNEL_BITS)
  @Range(0.0, 1.0)
  public var red: Float = 1f

  @Doc("Base color green, linear, 0 to 1.")
  @Quantized(CHANNEL_BITS)
  @Range(0.0, 1.0)
  public var green: Float = 1f

  @Doc("Base color blue, linear, 0 to 1.")
  @Quantized(CHANNEL_BITS)
  @Range(0.0, 1.0)
  public var blue: Float = 1f

  @Doc("How rough the surface is: 0 is mirror-smooth, 1 fully diffuse.")
  @Quantized(CHANNEL_BITS)
  @Range(0.0, 1.0)
  public var roughness: Float = DEFAULT_ROUGHNESS

  @Doc("How metallic the surface is: 0 is a dielectric, 1 a metal.")
  @Quantized(CHANNEL_BITS)
  @Range(0.0, 1.0)
  public var metallic: Float = 0f

  @Doc("Emitted light, red, linear.") public var emissiveRed: Float = 0f

  @Doc("Emitted light, green, linear.") public var emissiveGreen: Float = 0f

  @Doc("Emitted light, blue, linear.") public var emissiveBlue: Float = 0f

  override fun type(): ComponentType<Material> = Material

  /** The component type, and a factory for a material of a color. */
  public companion object : ComponentType<Material>() {
    /** Bits each color channel and factor takes on the wire. */
    public const val CHANNEL_BITS: Int = 8

    /** The roughness a material has unless told otherwise. */
    public const val DEFAULT_ROUGHNESS: Float = 0.5f

    /** A material of base color [red], [green], [blue] and [roughness]. */
    public fun of(
      red: Float,
      green: Float,
      blue: Float,
      roughness: Float = DEFAULT_ROUGHNESS,
    ): Material =
      Material().also {
        it.red = red
        it.green = green
        it.blue = blue
        it.roughness = roughness
      }
  }
}
