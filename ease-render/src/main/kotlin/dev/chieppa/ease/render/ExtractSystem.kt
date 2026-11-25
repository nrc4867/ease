package dev.chieppa.ease.render

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.ecs.prediction.CorrectionOffsets

/**
 * Copies every entity with a [LocalTransform] and a [Quad] into [snapshot] once per tick, each
 * drawn [offsets] away from its position while a corrected prediction settles. Register it in
 * `Phase.EXTRACT`, after everything that moves entities.
 */
public class ExtractSystem(
  private val snapshot: RenderSnapshot,
  private val offsets: CorrectionOffsets? = null,
) : IteratingSystem(family { all(LocalTransform, Quad) }) {
  override fun onTick() {
    snapshot.begin()
    super.onTick()
  }

  override fun onTickEntity(entity: Entity) {
    val transform = entity[LocalTransform]
    val x = transform.x + (offsets?.x(entity.id) ?: 0f)
    val y = transform.y + (offsets?.y(entity.id) ?: 0f)
    snapshot.write(entity.id, x, y, entity[Quad])
  }
}
