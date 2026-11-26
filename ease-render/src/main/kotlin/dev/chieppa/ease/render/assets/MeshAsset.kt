package dev.chieppa.ease.render.assets

import dev.chieppa.ease.gpu.mesh.MeshData
import dev.chieppa.ease.gpu.mesh.SkinVertices

/**
 * A cooked mesh loaded: its [mesh], ready to draw, its bounds, and its other vertex streams.
 * [joints] holds four joint indices a vertex and [weights] their four weights, which [skin] holds
 * in native memory for the lit pass to draw the mesh skinned; [uv1] two floats a vertex; [colors]
 * linear RGBA, four floats a vertex. Each is null when the mesh has none.
 */
@Suppress("LongParameterList") // The mesh and each optional stream it carries.
public class MeshAsset(
  public val mesh: MeshData,
  public val boundsMin: FloatArray,
  public val boundsMax: FloatArray,
  public val joints: IntArray?,
  public val weights: FloatArray?,
  public val uv1: FloatArray?,
  public val colors: FloatArray?,
) : AutoCloseable {
  /** The joints and weights as the lit pass draws them, or null when the mesh is not skinned. */
  public val skin: SkinVertices? =
    if (joints != null && weights != null) SkinVertices(joints, weights) else null

  /** Frees the mesh's native memory and its GPU copies. */
  override fun close() {
    mesh.close()
    skin?.close()
  }
}
