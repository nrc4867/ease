package dev.chieppa.ease.render

import dev.chieppa.ease.assets.loading.AssetStore
import dev.chieppa.ease.gpu.Color
import dev.chieppa.ease.gpu.CommandEncoder
import dev.chieppa.ease.gpu.GpuContext
import dev.chieppa.ease.gpu.RenderTarget
import dev.chieppa.ease.gpu.lit.LitMeshPass
import dev.chieppa.ease.gpu.lit.MaterialFloats
import dev.chieppa.ease.gpu.lit.MeshFrame
import dev.chieppa.ease.gpu.mesh.MeshData
import dev.chieppa.ease.render.scene.MeshShape
import dev.chieppa.ease.render.textures.MaterialTextureCache

/** Meshes a renderer draws at most unless told otherwise. */
public const val DEFAULT_DRAW_CAPACITY: Int = 4096

/**
 * Draws a [RenderSnapshot]: the target cleared to [clear], then every mesh the snapshot holds,
 * placed between its previous and current tick, seen through the snapshot's camera and lit by its
 * light, in the lit material template with a depth buffer. With no camera it only clears. With a
 * camera whose lens has a [LensProblem] it only clears and hands [problems] the problem's message,
 * once until the lens changes. A mesh draws with its `SurfaceTextures`, or else with the textures
 * of its material asset, loaded through [assets] (see [MaterialTextureCache]); a skinned mesh draws
 * bent by the joint matrices the snapshot holds for it. Holds room for [capacity] meshes and the
 * built-in shapes' meshes, made once, so drawing a frame allocates nothing on the heap.
 */
public class WorldRenderer(
  context: GpuContext,
  public val capacity: Int = DEFAULT_DRAW_CAPACITY,
  clear: Color = MeshFrame.DEFAULT_CLEAR,
  private val problems: (String) -> Unit = {},
  assets: AssetStore? = null,
) : AutoCloseable {
  private val materialTextures = assets?.let { MaterialTextureCache(context, it) }

  private val pass = LitMeshPass(context, capacity)
  private val frame = MeshFrame(capacity).also { it.clear = clear }
  private val shapes = MeshShape.entries.map(Primitives::build).toTypedArray()
  private val view = CameraView()
  private val transform = FloatArray(TRANSFORM_FLOATS)
  private val draws = DrawTransforms()
  private var reported: LensProblem? = null

  /**
   * Records into [encoder] a frame of [snapshot] in [target], each entity [alpha] of the way from
   * its previous tick's transform to its current one. Fails when the snapshot holds more than
   * [capacity] meshes.
   */
  public fun render(
    snapshot: RenderSnapshot,
    alpha: Float,
    target: RenderTarget,
    encoder: CommandEncoder,
  ) {
    check(snapshot.count <= capacity) {
      "${snapshot.count} meshes do not fit a renderer for $capacity"
    }
    frame.clearDraws()
    val camera = snapshot.camera
    val problem = if (camera.present) camera.problem else null
    if (problem != reported) problem?.let { problems(it.message) }
    reported = problem
    if (camera.present && problem == null) {
      snapshot.transformInto(camera.entity, alpha, transform, 0)
      view.write(transform, camera, target.size, frame)
      LightSetup.write(snapshot, alpha, transform, frame)
      for (index in 0 until snapshot.count) addDraw(snapshot, snapshot.entityAt(index), alpha)
    }
    pass.record(encoder, target, frame)
    materialTextures?.endFrame()
  }

  private fun addDraw(snapshot: RenderSnapshot, id: Int, alpha: Float) {
    val mesh: MeshData = snapshot.customMesh(id) ?: shapes[checkNotNull(snapshot.shape(id)).ordinal]
    val index = frame.add(mesh)
    val asset = snapshot.materialAssets[id]
    val textures = snapshot.textures(id) ?: asset?.let { materialTextures?.of(it) }
    frame.textures(index, textures)
    val skins = snapshot.skins
    skins.skin(id)?.let {
      frame.skin(index, it, skins.palette, skins.paletteFrom(id), skins.jointCount(id))
    }
    snapshot.transformInto(id, alpha, transform, 0)
    draws.write(transform, frame.models, frame.normals, index)
    System.arraycopy(
      snapshot.materials,
      id * MaterialFloats.COUNT,
      frame.materials,
      index * MaterialFloats.COUNT,
      MaterialFloats.COUNT,
    )
  }

  override fun close() {
    materialTextures?.close()
    pass.close()
    shapes.forEach(MeshData::close)
  }
}
