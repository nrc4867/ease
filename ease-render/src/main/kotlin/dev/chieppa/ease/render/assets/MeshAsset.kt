package dev.chieppa.ease.render.assets

import dev.chieppa.ease.gpu.mesh.MeshData

/**
 * A cooked mesh loaded: its [mesh], ready to draw, its bounds, and the vertex streams the lit pass
 * does not draw yet, kept for skinning and later passes. [joints] holds four joint indices a vertex
 * and [weights] their four weights; [uv1] two floats a vertex; [colors] linear RGBA, four floats a
 * vertex. Each is null when the mesh has none.
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
  /** Frees the mesh's native memory and its GPU copy. */
  override fun close() {
    mesh.close()
  }
}
