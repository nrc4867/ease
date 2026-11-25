package dev.chieppa.ease.render

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.ecs.prediction.CorrectionOffsets
import dev.chieppa.ease.render.scene.Camera
import dev.chieppa.ease.render.scene.CustomMesh
import dev.chieppa.ease.render.scene.DirectionalLight
import dev.chieppa.ease.render.scene.Mesh

/**
 * Copies every entity with a [LocalTransform] and a [Mesh], [CustomMesh], [Camera] or
 * [DirectionalLight] into [snapshot] once per tick, each drawn [offsets] away from its position
 * while a corrected prediction settles. Register it in `Phase.EXTRACT`, after everything that moves
 * entities.
 */
public class ExtractSystem(
  private val snapshot: RenderSnapshot,
  private val offsets: CorrectionOffsets? = null,
) :
  IteratingSystem(
    family {
      all(LocalTransform)
      any(Mesh, CustomMesh, Camera, DirectionalLight)
    }
  ) {
  private val extract = EntityExtract(snapshot)

  override fun onTick() {
    snapshot.begin()
    super.onTick()
  }

  override fun onTickEntity(entity: Entity) {
    val id = entity.id
    val dx = offsets?.x(id) ?: 0f
    val dy = offsets?.y(id) ?: 0f
    val dz = offsets?.z(id) ?: 0f
    extract.write(world, entity, dx, dy, dz)
  }
}
