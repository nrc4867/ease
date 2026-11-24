package dev.chieppa.ease.sample

import com.github.quillraven.fleks.Component
import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.render.Quad

private const val RED_SHIFT = 16
private const val GREEN_SHIFT = 8
private const val CHANNEL = 0xFF
private const val CHANNEL_MAX = 255f

/**
 * One of the sample's starting squares: where it starts, how fast it moves, how big it is and its
 * color as `0xRRGGBB`.
 */
public class Square(
  public val x: Float,
  public val y: Float,
  public val speedX: Float,
  public val speedY: Float,
  public val size: Float,
  public val rgb: Int,
) {
  /** The components an entity for this square starts with. */
  public fun components(): List<Component<out Any>> =
    listOf(
      LocalTransform().also {
        it.x = x
        it.y = y
      },
      Quad.of(size, channel(RED_SHIFT), channel(GREEN_SHIFT), channel(0)),
      Velocity.of(speedX, speedY),
    )

  private fun channel(shift: Int): Float = (rgb shr shift and CHANNEL) / CHANNEL_MAX
}
