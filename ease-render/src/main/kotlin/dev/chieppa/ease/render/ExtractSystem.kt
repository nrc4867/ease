package dev.chieppa.ease.render

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import dev.chieppa.ease.animation.runtime.AnimationAssets
import dev.chieppa.ease.animation.skinning.SkinPalettes
import dev.chieppa.ease.assets.loading.AssetStore
import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.ecs.prediction.CorrectionOffsets
import dev.chieppa.ease.render.scene.Camera
import dev.chieppa.ease.render.scene.CustomMesh
import dev.chieppa.ease.render.scene.DirectionalLight
import dev.chieppa.ease.render.scene.Mesh
import dev.chieppa.ease.render.scene.MeshRef
import dev.chieppa.ease.scene.transform.WorldTransforms

/**
 * Copies every entity with a [LocalTransform] and a [Mesh], [MeshRef], [CustomMesh], [Camera] or
 * [DirectionalLight] into [snapshot] once per tick at its world transform, each drawn [offsets]
 * away from its position while a corrected prediction settles. Mesh and material assets load
 * through [assets]; one that fails is listed in [assetProblems] and not drawn until the store next
 * reloads something, when it is tried again. Skinned meshes are written with their joint matrices
 * this tick. Register it in `Phase.EXTRACT`, after everything that moves entities and after the
 * world's `WorldTransforms`.
 */
public class ExtractSystem(
  private val snapshot: RenderSnapshot,
  private val offsets: CorrectionOffsets? = null,
  assets: AssetStore? = null,
) :
  IteratingSystem(
    family {
      all(LocalTransform)
      any(Mesh, MeshRef, CustomMesh, Camera, DirectionalLight)
    }
  ) {
  private val drawAssets = DrawAssets(assets)
  private val extract =
    EntityExtract(snapshot, drawAssets, SkinPalettes(assets?.let(AnimationAssets::of)))
  private var worlds: WorldTransforms? = null

  /** The mesh and material assets that failed to load, one line each, until they are retried. */
  public val assetProblems: Collection<String>
    get() = drawAssets.problems

  override fun onInit() {
    worlds = world.systemOrNull<WorldTransforms>()
  }

  override fun onTick() {
    drawAssets.retryAfterReloads()
    snapshot.begin()
    super.onTick()
  }

  override fun onTickEntity(entity: Entity) {
    val id = entity.id
    val dx = offsets?.x(id) ?: 0f
    val dy = offsets?.y(id) ?: 0f
    val dz = offsets?.z(id) ?: 0f
    extract.write(world, entity, dx, dy, dz, worlds)
  }
}
