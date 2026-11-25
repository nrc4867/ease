package dev.chieppa.ease.render

import dev.chieppa.ease.gpu.mesh.MeshData
import dev.chieppa.ease.render.scene.MeshShape
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val HALF = 0.5f
private const val CUBE_FACES = 6
private const val SPHERE_SEGMENTS = 32
private const val SPHERE_RINGS = 16

/** The fourth corner of a square face, counting from 0. */
private const val LAST_CORNER = 3

/**
 * The meshes of the built-in [MeshShape]s, each fitting a unit box centered on the origin, with
 * counter-clockwise front faces and outward normals.
 */
public object Primitives {
  /** A new mesh of [shape], committed. */
  public fun build(shape: MeshShape): MeshData =
    MeshData().also { mesh ->
      when (shape) {
        MeshShape.CUBE -> cube(mesh)
        MeshShape.PLANE -> plane(mesh)
        MeshShape.SPHERE -> sphere(mesh)
      }
      mesh.commit()
    }

  /** Adds a cube with sides 1 long to [mesh]: four vertices a face, so each face is flat. */
  public fun cube(mesh: MeshData) {
    for (face in 0 until CUBE_FACES) {
      val axis = face / 2
      val sign = if (face % 2 == 0) 1f else -1f
      val normal = FloatArray(3).also { it[axis] = sign }
      // Two axes spanning the face, chosen so u x v points along the normal.
      val u = FloatArray(3).also { it[(axis + 1) % 3] = sign }
      val v = FloatArray(3).also { it[(axis + 2) % 3] = 1f }
      val first = mesh.vertexCount
      for ((su, sv) in CORNERS) {
        mesh.vertex(
          normal[0] * HALF + (u[0] * su + v[0] * sv) * HALF,
          normal[1] * HALF + (u[1] * su + v[1] * sv) * HALF,
          normal[2] * HALF + (u[2] * su + v[2] * sv) * HALF,
          normal[0],
          normal[1],
          normal[2],
          (su + 1) * HALF,
          (1 - sv) * HALF,
        )
      }
      mesh.triangle(first, first + 1, first + 2)
      mesh.triangle(first, first + 2, first + LAST_CORNER)
    }
  }

  /** Adds a square 1 on a side in the x-z plane, facing up +y, to [mesh]. */
  public fun plane(mesh: MeshData) {
    val first = mesh.vertexCount
    mesh.vertex(-HALF, 0f, HALF, 0f, 1f, 0f, 0f, 1f)
    mesh.vertex(HALF, 0f, HALF, 0f, 1f, 0f, 1f, 1f)
    mesh.vertex(HALF, 0f, -HALF, 0f, 1f, 0f, 1f, 0f)
    mesh.vertex(-HALF, 0f, -HALF, 0f, 1f, 0f, 0f, 0f)
    mesh.triangle(first, first + 1, first + 2)
    mesh.triangle(first, first + 2, first + LAST_CORNER)
  }

  /** Adds a sphere 1 across to [mesh], in rings of latitude from the top. */
  public fun sphere(mesh: MeshData) {
    val first = mesh.vertexCount
    for (ring in 0..SPHERE_RINGS) {
      val polar = PI * ring / SPHERE_RINGS
      val y = cos(polar).toFloat()
      val radius = sin(polar).toFloat()
      for (segment in 0..SPHERE_SEGMENTS) {
        val azimuth = 2 * PI * segment / SPHERE_SEGMENTS
        val x = radius * sin(azimuth).toFloat()
        val z = radius * cos(azimuth).toFloat()
        val u = segment.toFloat() / SPHERE_SEGMENTS
        val v = ring.toFloat() / SPHERE_RINGS
        mesh.vertex(x * HALF, y * HALF, z * HALF, x, y, z, u, v)
      }
    }
    val row = SPHERE_SEGMENTS + 1
    for (ring in 0 until SPHERE_RINGS) {
      for (segment in 0 until SPHERE_SEGMENTS) {
        val a = first + ring * row + segment
        val b = a + row
        if (ring != 0) mesh.triangle(a, b, a + 1)
        if (ring != SPHERE_RINGS - 1) mesh.triangle(a + 1, b, b + 1)
      }
    }
  }

  /** A face's corners in its own axes, counter-clockwise seen from outside. */
  private val CORNERS = listOf(-1f to -1f, 1f to -1f, 1f to 1f, -1f to 1f)
}
