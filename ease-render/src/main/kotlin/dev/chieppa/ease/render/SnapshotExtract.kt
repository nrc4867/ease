package dev.chieppa.ease.render

import com.github.quillraven.fleks.World
import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.render.scene.Camera
import dev.chieppa.ease.render.scene.CustomMesh
import dev.chieppa.ease.render.scene.DirectionalLight
import dev.chieppa.ease.render.scene.Mesh

/**
 * Writes every entity of [world] that [ExtractSystem] would copy into this snapshot as one new
 * extract, outside any tick: how a host draws a world nobody steps, such as a capture of a frozen
 * world. The world itself is only read.
 */
public fun RenderSnapshot.extractFrom(world: World) {
  val drawn = world.family {
    all(LocalTransform)
    any(Mesh, CustomMesh, Camera, DirectionalLight)
  }
  val extract = EntityExtract(this)
  begin()
  drawn.forEach { entity -> extract.write(world, entity, 0f, 0f, 0f) }
}
