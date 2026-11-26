package dev.chieppa.ease.physics.jolt

import com.github.stephengold.joltjni.BoxShape
import com.github.stephengold.joltjni.CapsuleShape
import com.github.stephengold.joltjni.MeshShapeSettings
import com.github.stephengold.joltjni.Quat
import com.github.stephengold.joltjni.RotatedTranslatedShape
import com.github.stephengold.joltjni.SphereShape
import com.github.stephengold.joltjni.Vec3
import com.github.stephengold.joltjni.readonly.ConstShape
import dev.chieppa.ease.physics.body.Collider
import dev.chieppa.ease.physics.body.ShapeKind
import dev.chieppa.ease.physics.body.TriangleMesh
import java.lang.foreign.Arena
import java.lang.foreign.ValueLayout
import java.nio.ByteOrder

/** Jolt shapes for colliders and characters. The caller closes what it is given. */
internal object JoltShapes {
  /** Jolt's default rounding of convex shapes' edges, in world units. */
  private const val CONVEX_RADIUS = 0.05f

  /** The smallest size a shape is given, so Jolt's checks on its dimensions pass. */
  private const val MIN_SIZE = 1.0e-3f

  /** Corners of a triangle, and coordinates of a corner. */
  private const val CORNERS = 3

  /** Floats one triangle of a triangle list takes. */
  private const val TRIANGLE_FLOATS = 9

  /** [collider]'s shape, centered on its node. */
  fun of(collider: Collider): ConstShape =
    when (collider.shape) {
      ShapeKind.BOX -> box(collider.halfX, collider.halfY, collider.halfZ)
      ShapeKind.SPHERE -> SphereShape(collider.radius.coerceAtLeast(MIN_SIZE))
      ShapeKind.CAPSULE -> capsule(collider.radius, collider.halfHeight)
    }

  /**
   * A static mesh shape of [mesh]'s triangles. They are written as a triangle list into native
   * memory, which reaches jolt-jni as a direct buffer only inside the call that copies it into
   * Jolt's settings; the memory is freed before this returns, and Jolt keeps only its own copy.
   */
  fun mesh(mesh: TriangleMesh): ConstShape {
    require(mesh.triangleCount > 0) { "A mesh collider needs at least one triangle" }
    val settings =
      Arena.ofConfined().use { arena ->
        val floats = mesh.triangleCount.toLong() * TRIANGLE_FLOATS
        val soup = arena.allocate(ValueLayout.JAVA_FLOAT, floats)
        var at = 0L
        for (triangle in 0 until mesh.triangleCount) {
          for (corner in 0 until CORNERS) {
            val vertex = mesh.corner(triangle, corner)
            for (axis in 0 until CORNERS) {
              soup.setAtIndex(ValueLayout.JAVA_FLOAT, at++, mesh.position(vertex, axis))
            }
          }
        }
        val buffer = soup.asByteBuffer().order(ByteOrder.nativeOrder()).asFloatBuffer()
        MeshShapeSettings(buffer)
      }
    return settings.use { built ->
      built.create().use { result ->
        check(!result.hasError()) { "Jolt could not build a mesh shape: ${result.error}" }
        result.get()
      }
    }
  }

  /** An upright capsule [height] tall and [radius] round whose bottom is at its origin. */
  fun standingCapsule(radius: Float, height: Float): ConstShape {
    val round = radius.coerceAtLeast(MIN_SIZE)
    val halfHeight = (height / 2 - round).coerceAtLeast(MIN_SIZE)
    return CapsuleShape(halfHeight, round).use { inner ->
      RotatedTranslatedShape(Vec3(0f, halfHeight + round, 0f), Quat(), inner)
    }
  }

  private fun box(halfX: Float, halfY: Float, halfZ: Float): ConstShape {
    val x = halfX.coerceAtLeast(MIN_SIZE)
    val y = halfY.coerceAtLeast(MIN_SIZE)
    val z = halfZ.coerceAtLeast(MIN_SIZE)
    return BoxShape(x, y, z, minOf(CONVEX_RADIUS, x, y, z))
  }

  private fun capsule(radius: Float, halfHeight: Float): ConstShape {
    val round = radius.coerceAtLeast(MIN_SIZE)
    return if (halfHeight < MIN_SIZE) SphereShape(round) else CapsuleShape(halfHeight, round)
  }
}
