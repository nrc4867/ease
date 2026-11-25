package dev.chieppa.ease.sample

import com.github.quillraven.fleks.Component
import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.render.scene.Material
import dev.chieppa.ease.render.scene.Mesh
import dev.chieppa.ease.render.scene.MeshShape

/**
 * A cube the sample spawns in code, such as a courier: where it starts, how fast it moves, how big
 * it is and its linear base color. The playfield scene's cubes are in its node files.
 */
@Suppress("LongParameterList") // Named arguments: where, how fast, how big, what color.
public class Cube(
  public val x: Float,
  public val y: Float,
  public val speedX: Float,
  public val speedY: Float,
  public val size: Float,
  public val red: Float,
  public val green: Float,
  public val blue: Float,
) {
  /** The components an entity for this cube starts with. */
  public fun components(): List<Component<out Any>> =
    listOf(
      LocalTransform().also {
        it.x = x
        it.y = y
        it.scaleX = size
        it.scaleY = size
        it.scaleZ = size
      },
      Mesh.of(MeshShape.CUBE),
      Material.of(red, green, blue),
      Velocity.of(speedX, speedY),
    )
}
