package dev.chieppa.ease.physics.jolt

import com.github.stephengold.joltjni.BoxShape
import com.github.stephengold.joltjni.CapsuleShape
import com.github.stephengold.joltjni.Quat
import com.github.stephengold.joltjni.RotatedTranslatedShape
import com.github.stephengold.joltjni.SphereShape
import com.github.stephengold.joltjni.Vec3
import com.github.stephengold.joltjni.readonly.ConstShape
import dev.chieppa.ease.physics.body.Collider
import dev.chieppa.ease.physics.body.ShapeKind

/** Jolt shapes for colliders and characters. The caller closes what it is given. */
internal object JoltShapes {
  /** Jolt's default rounding of convex shapes' edges, in world units. */
  private const val CONVEX_RADIUS = 0.05f

  /** The smallest size a shape is given, so Jolt's checks on its dimensions pass. */
  private const val MIN_SIZE = 1.0e-3f

  /** [collider]'s shape, centered on its node. */
  fun of(collider: Collider): ConstShape =
    when (collider.shape) {
      ShapeKind.BOX -> box(collider.halfX, collider.halfY, collider.halfZ)
      ShapeKind.SPHERE -> SphereShape(collider.radius.coerceAtLeast(MIN_SIZE))
      ShapeKind.CAPSULE -> capsule(collider.radius, collider.halfHeight)
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
