package dev.chieppa.ease.render

import com.github.quillraven.fleks.World
import dev.chieppa.ease.ecs.LocalTransform

/**
 * Writes every entity of [world] with a [LocalTransform] and a [Quad] into this snapshot as one new
 * extract, outside any tick: how a host draws a world nobody steps, such as a capture of a frozen
 * world. The world itself is only read.
 */
public fun RenderSnapshot.extractFrom(world: World) {
  val quads = world.family { all(LocalTransform, Quad) }
  begin()
  quads.forEach { entity ->
    val transform = entity[LocalTransform]
    write(entity.id, transform.x, transform.y, entity[Quad])
  }
}
