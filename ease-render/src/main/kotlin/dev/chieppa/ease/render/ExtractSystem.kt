package dev.chieppa.ease.render

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import dev.chieppa.ease.ecs.LocalTransform

/**
 * Copies every entity with a [LocalTransform] and a [Quad] into [snapshot] once per tick. Register
 * it in `Phase.EXTRACT`, after everything that moves entities.
 */
public class ExtractSystem(private val snapshot: RenderSnapshot) :
  IteratingSystem(family { all(LocalTransform, Quad) }) {
  override fun onTick() {
    snapshot.begin()
    super.onTick()
  }

  override fun onTickEntity(entity: Entity) {
    val transform = entity[LocalTransform]
    snapshot.write(entity.id, transform.x, transform.y, entity[Quad])
  }
}
