package dev.chieppa.ease.render

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.gpu.lit.MaterialFloats
import dev.chieppa.ease.render.scene.Camera
import dev.chieppa.ease.render.scene.CustomMesh
import dev.chieppa.ease.render.scene.DirectionalLight
import dev.chieppa.ease.render.scene.Material
import dev.chieppa.ease.render.scene.Mesh
import dev.chieppa.ease.render.scene.SurfaceTextures

/**
 * Copies one entity into a [RenderSnapshot]: its transform, moved by a correction offset, and
 * whichever of a mesh, a camera and a light it carries. One per snapshot writer, so copying
 * allocates nothing.
 */
internal class EntityExtract(private val snapshot: RenderSnapshot) {
  private val transform = FloatArray(TRANSFORM_FLOATS)

  /** Writes [entity] of [world], drawn ([dx], [dy], [dz]) from where it is. */
  fun write(world: World, entity: Entity, dx: Float, dy: Float, dz: Float) =
    with(world) {
      val local = entity[LocalTransform]
      transform[0] = local.x + dx
      transform[1] = local.y + dy
      transform[2] = local.z + dz
      transform[ROTATION_AT] = local.rotationX
      transform[ROTATION_AT + 1] = local.rotationY
      transform[ROTATION_AT + 2] = local.rotationZ
      transform[ROTATION_AT + QUAT_W] = local.rotationW
      transform[SCALE_AT] = local.scaleX
      transform[SCALE_AT + 1] = local.scaleY
      transform[SCALE_AT + 2] = local.scaleZ
      val id = entity.id
      snapshot.writeTransform(id, transform)
      val custom = entity.getOrNull(CustomMesh)
      val mesh = entity.getOrNull(Mesh)
      if (custom != null || mesh != null) {
        writeMaterial(id, entity.getOrNull(Material))
        val shape = if (custom == null) mesh?.shape else null
        snapshot.addMesh(id, shape, custom?.mesh, entity.getOrNull(SurfaceTextures)?.textures)
      }
      entity.getOrNull(Camera)?.let { writeCamera(id, it) }
      entity.getOrNull(DirectionalLight)?.let { writeLight(id, it) }
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
