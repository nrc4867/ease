package dev.chieppa.ease.sdf.mesh

import dev.chieppa.ease.sdf.SdfConstants.AXES

/**
 * A distance field's surface as triangles: [positions] and unit [normals], three floats per vertex,
 * and [indices], three per triangle, counter-clockwise seen from outside the solid. The same field
 * and grid always give the same arrays, bit for bit, on every machine.
 */
public class SdfMesh(
  public val positions: FloatArray,
  public val normals: FloatArray,
  public val indices: IntArray,
) {
  /** How many vertices the mesh has. */
  public val vertexCount: Int
    get() = positions.size / AXES

  /** How many triangles the mesh has. */
  public val triangleCount: Int
    get() = indices.size / AXES
}
