package dev.chieppa.ease.render

import com.github.quillraven.fleks.World
import dev.chieppa.ease.animation.runtime.AnimationAssets
import dev.chieppa.ease.animation.skinning.SkinPalettes
import dev.chieppa.ease.assets.loading.AssetStore
import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.render.scene.Camera
import dev.chieppa.ease.render.scene.CustomMesh
import dev.chieppa.ease.render.scene.DirectionalLight
import dev.chieppa.ease.render.scene.Mesh
import dev.chieppa.ease.render.scene.MeshRef
import dev.chieppa.ease.scene.transform.WorldTransforms

/**
 * Writes every entity of [world] that [ExtractSystem] would copy into this snapshot as one new
 * extract, outside any tick: how a host draws a world nobody steps, such as a capture of a frozen
 * world. The world itself is only read; mesh and material assets load through [assets].
 */
public fun RenderSnapshot.extractFrom(world: World, assets: AssetStore? = null) {
  val drawn = world.family {
    all(LocalTransform)
    any(Mesh, MeshRef, CustomMesh, Camera, DirectionalLight)
  }
  val extract =
    EntityExtract(this, DrawAssets(assets), SkinPalettes(assets?.let(AnimationAssets::of)))
  val worlds = world.systemOrNull<WorldTransforms>()
  worlds?.invalidate()
  begin()
  drawn.forEach { entity -> extract.write(world, entity, 0f, 0f, 0f, worlds) }
}
