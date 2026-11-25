package dev.chieppa.ease.ecs.replication

import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.ecs.LocalTransformReplicated
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * An interpolated rotation group blends as one quaternion, in generated and schema serializers
 * alike: the shorter way round and of unit length, even between a rotation and its negation.
 */
class RotationBlendTest {
  private fun transform(x: Float, y: Float, z: Float, w: Float) =
    LocalTransform().also {
      it.rotationX = x
      it.rotationY = y
      it.rotationZ = z
      it.rotationW = w
    }

  private fun length(t: LocalTransform): Float =
    sqrt(
      t.rotationX * t.rotationX +
        t.rotationY * t.rotationY +
        t.rotationZ * t.rotationZ +
        t.rotationW * t.rotationW
    )

  @Test
  fun `a rotation blended toward its own negation stays put and of unit length`() {
    val into = LocalTransform()

    LocalTransformReplicated.interpolate(
      transform(0f, 0f, 0f, 1f),
      transform(0f, 0f, 0f, -1f),
      0.5f,
      into,
    )

    assertEquals(1f, length(into), TOLERANCE)
    assertEquals(1f, into.rotationW, TOLERANCE)
  }

  @Test
  fun `halfway through a quarter turn is an eighth turn of unit length`() {
    val into = LocalTransform()
    val quarter = transform(0f, 0f, HALF_SQRT2, HALF_SQRT2)

    LocalTransformReplicated.interpolate(transform(0f, 0f, 0f, 1f), quarter, 0.5f, into)

    assertEquals(1f, length(into), TOLERANCE)
    assertEquals(SIN_EIGHTH, into.rotationZ, TOLERANCE)
    assertEquals(COS_EIGHTH, into.rotationW, TOLERANCE)
  }

  @Test
  fun `a schema rotation group blends as the generated one does`() {
    val schema =
      replicatedSchema("test.Spinner") {
        listOf("x", "y", "z", "w").forEach { float(it, interpolated = true, rotation = "turn") }
      }
    fun spin(z: Float, w: Float) =
      schema.component().also {
        it.setFloat(2, z)
        it.setFloat(3, w)
      }
    val into = schema.component()

    schema.serializer.interpolate(spin(0f, 1f), spin(0f, -1f), 0.5f, into)
    val halfway = schema.component()
    schema.serializer.interpolate(spin(0f, 1f), spin(HALF_SQRT2, HALF_SQRT2), 0.5f, halfway)

    assertEquals(1f, into.getFloat(3), TOLERANCE)
    assertEquals(SIN_EIGHTH, halfway.getFloat(2), TOLERANCE)
    assertEquals(COS_EIGHTH, halfway.getFloat(3), TOLERANCE)
  }

  private companion object {
    const val TOLERANCE = 1e-5f
    val HALF_SQRT2 = sqrt(0.5f)

    /** sin and cos of an eighth of a half turn: the quaternion of an eighth turn. */
    const val SIN_EIGHTH = 0.38268343f
    const val COS_EIGHTH = 0.9238795f
  }
}
