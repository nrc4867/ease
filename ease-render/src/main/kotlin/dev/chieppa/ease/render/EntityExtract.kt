package dev.chieppa.ease.render

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import dev.chieppa.ease.gpu.lit.MaterialFloats
import dev.chieppa.ease.render.assets.MaterialAsset
import dev.chieppa.ease.render.scene.Camera
import dev.chieppa.ease.render.scene.CustomMesh
import dev.chieppa.ease.render.scene.DirectionalLight
import dev.chieppa.ease.render.scene.Material
import dev.chieppa.ease.render.scene.MaterialRef
import dev.chieppa.ease.render.scene.Mesh
import dev.chieppa.ease.render.scene.MeshRef
import dev.chieppa.ease.render.scene.SurfaceTextures
import dev.chieppa.ease.scene.transform.WorldTransforms

/** Color channels of a material's base and emitted colors. */
private const val COLOR_CHANNELS = 3

/**
 * Copies one entity into a [RenderSnapshot]: its world transform (its local one placed by its
 * parents through [WorldTransforms], when the world has it), moved by a correction offset, and
 * whichever of a mesh, a camera and a light it carries. A [MeshRef] draws its mesh asset in place
 * of any built-in [Mesh], and a [MaterialRef] its material asset in place of any inline [Material];
 * the assets come through [assets]. One per snapshot writer, so copying allocates nothing.
 */
internal class EntityExtract(private val snapshot: RenderSnapshot, private val assets: DrawAssets) {
  private val transform = FloatArray(TRANSFORM_FLOATS)

  /** Writes [entity] of [world], drawn ([dx], [dy], [dz]) from where it is. */
  @Suppress("LongParameterList") // The offset as three floats, so nothing allocates.
  fun write(
    world: World,
    entity: Entity,
    dx: Float,
    dy: Float,
    dz: Float,
    worlds: WorldTransforms?,
  ) =
    with(world) {
      if (worlds != null) worlds.write(entity, transform, 0)
      else WorldTransforms.writeLocal(world, entity, transform, 0)
      transform[0] += dx
      transform[1] += dy
      transform[2] += dz
      val id = entity.id
      snapshot.writeTransform(id, transform)
      val custom = entity.getOrNull(CustomMesh)?.mesh
      val asset = entity.getOrNull(MeshRef)?.mesh?.let { assets.mesh(it.guid)?.mesh }
      val shape = entity.getOrNull(Mesh)?.shape
      val builtIn = shape != null && entity.getOrNull(MeshRef) == null
      if (custom != null || asset != null || builtIn) {
        val materialAsset =
          entity.getOrNull(MaterialRef)?.material?.let { assets.material(it.guid) }
        if (materialAsset != null) writeAssetMaterial(id, materialAsset)
        else writeMaterial(id, entity.getOrNull(Material))
        val textures = entity.getOrNull(SurfaceTextures)?.textures
        val drawnShape = if (custom == null && asset == null) shape else null
        snapshot.addMesh(id, drawnShape, custom ?: asset, textures)
        snapshot.materialAssets[id] = materialAsset
      }
      entity.getOrNull(Camera)?.let { writeCamera(id, it) }
      entity.getOrNull(DirectionalLight)?.let { writeLight(id, it) }
    }

  private fun writeAssetMaterial(id: Int, material: MaterialAsset) {
    val values = snapshot.materials
    val at = id * MaterialFloats.COUNT
    for (channel in 0 until COLOR_CHANNELS) {
      values[at + MaterialFloats.BASE_COLOR + channel] = material.baseColor[channel]
      values[at + MaterialFloats.EMISSIVE + channel] = material.emissive[channel]
    }
    values[at + MaterialFloats.ROUGHNESS] = material.roughness
    values[at + MaterialFloats.METALLIC] = material.metallic
  }

  private fun writeMaterial(id: Int, material: Material?) {
    val values = snapshot.materials
    val at = id * MaterialFloats.COUNT
    values[at] = material?.red ?: 1f
    values[at + 1] = material?.green ?: 1f
    values[at + 2] = material?.blue ?: 1f
    values[at + MaterialFloats.ROUGHNESS] = material?.roughness ?: Material.DEFAULT_ROUGHNESS
    values[at + MaterialFloats.METALLIC] = material?.metallic ?: 0f
    values[at + MaterialFloats.EMISSIVE] = material?.emissiveRed ?: 0f
    values[at + MaterialFloats.EMISSIVE + 1] = material?.emissiveGreen ?: 0f
    values[at + MaterialFloats.EMISSIVE + 2] = material?.emissiveBlue ?: 0f
  }

  private fun writeCamera(id: Int, camera: Camera) {
    val found = snapshot.camera
    if (found.present && found.entity < id) return
    found.entity = id
    found.fieldOfView = camera.fieldOfView
    found.near = camera.near
    found.far = camera.far
    found.problem = LensProblem.of(camera.fieldOfView, camera.near, camera.far)
  }

  private fun writeLight(id: Int, light: DirectionalLight) {
    val found = snapshot.light
    if (found.present && found.entity < id) return
    found.entity = id
    found.color[0] = light.red
    found.color[1] = light.green
    found.color[2] = light.blue
    found.intensity = light.intensity
    found.ambient = light.ambient
  }
}
