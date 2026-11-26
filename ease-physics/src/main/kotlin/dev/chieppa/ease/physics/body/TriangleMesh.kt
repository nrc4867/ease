package dev.chieppa.ease.physics.body

/**
 * A triangle mesh for a [MeshCollider], in its node's space: [positions] holds x, y and z of each
 * vertex, and [indices] three vertex indices per triangle, counter-clockwise seen from the front.
 * Both arrays are copied in, so changing the caller's arrays later changes nothing here.
 */
public class TriangleMesh(positions: FloatArray, indices: IntArray) {
  private val positionValues: FloatArray = positions.copyOf()
  private val indexValues: IntArray = indices.copyOf()

  init {
    require(positions.size % AXES == 0) { "Positions come in threes, got ${positions.size}" }
    require(indices.size % CORNERS == 0) { "Indices come in threes, got ${indices.size}" }
    val vertices = positions.size / AXES
    val outside = indices.firstOrNull { it !in 0 until vertices }
    require(outside == null) { "Index $outside names a vertex the mesh's $vertices do not include" }
  }

  /** How many vertices the mesh has. */
  public val vertexCount: Int
    get() = positionValues.size / AXES

  /** How many triangles the mesh has. */
  public val triangleCount: Int
    get() = indexValues.size / CORNERS

  /** Coordinate [axis] (0 x, 1 y, 2 z) of vertex [vertex]. */
  public fun position(vertex: Int, axis: Int): Float = positionValues[vertex * AXES + axis]

  /** The vertex at corner [corner] (0 to 2) of triangle [triangle]. */
  public fun corner(triangle: Int, corner: Int): Int = indexValues[triangle * CORNERS + corner]

  private companion object {
    const val AXES = 3
    const val CORNERS = 3
  }
}
