package dev.chieppa.ease.sdf.field

import dev.chieppa.ease.math.Quat
import dev.chieppa.ease.sdf.SdfConstants.AXES
import dev.chieppa.ease.sdf.SdfConstants.ROW_FLOATS
import dev.chieppa.ease.sdf.shape.Box
import dev.chieppa.ease.sdf.shape.Capsule
import dev.chieppa.ease.sdf.shape.Combined
import dev.chieppa.ease.sdf.shape.CsgOp
import dev.chieppa.ease.sdf.shape.Cylinder
import dev.chieppa.ease.sdf.shape.Plane
import dev.chieppa.ease.sdf.shape.RoundedBox
import dev.chieppa.ease.sdf.shape.SdfShape
import dev.chieppa.ease.sdf.shape.Sphere
import dev.chieppa.ease.sdf.shape.Torus
import dev.chieppa.ease.sdf.shape.Transformed

/**
 * Flattens a shape tree into an [SdfProgram]: children before parents, a new register for every
 * result, so the same tree always gives the same program.
 */
internal class SdfCompiler private constructor() {
  private val instructions = ArrayList<Int>()
  private val nodes = ArrayList<Float>()
  private var points = 1
  private var distances = 0

  private fun compile(shape: SdfShape, point: Int): Int =
    when (shape) {
      is Sphere -> primitive(SdfOps.SPHERE, point, shape.radius)
      is Box -> primitive(SdfOps.BOX, point, shape.halfX, shape.halfY, shape.halfZ)
      is RoundedBox ->
        primitive(SdfOps.ROUNDED_BOX, point, shape.halfX, shape.halfY, shape.halfZ, shape.radius)
      is Capsule -> primitive(SdfOps.CAPSULE, point, shape.halfHeight, shape.radius)
      is Cylinder -> primitive(SdfOps.CYLINDER, point, shape.radius, shape.halfHeight)
      is Plane ->
        primitive(SdfOps.PLANE, point, shape.normalX, shape.normalY, shape.normalZ, shape.offset)
      is Torus -> primitive(SdfOps.TORUS, point, shape.majorRadius, shape.minorRadius)
      is Transformed -> transformed(shape, point)
      is Combined -> combined(shape, point)
    }

  private fun primitive(op: Int, point: Int, vararg values: Float): Int {
    val node = node(FloatArray(SdfOps.VALUES), values)
    return emit(op, distances++, point, 0, node)
  }

  private fun combined(shape: Combined, point: Int): Int {
    val first = compile(shape.first, point)
    val second = compile(shape.second, point)
    val op =
      when (shape.op) {
        CsgOp.UNION -> SdfOps.UNION
        CsgOp.SUBTRACT -> SdfOps.SUBTRACT
        CsgOp.INTERSECT -> SdfOps.INTERSECT
      }
    val node = node(FloatArray(SdfOps.VALUES), floatArrayOf(shape.blend))
    return emit(op, distances++, first, second, node)
  }

  private fun transformed(shape: Transformed, point: Int): Int {
    val node = node(inverseRows(shape), floatArrayOf(shape.scale))
    val local = emit(SdfOps.TRANSFORM, points++, point, 0, node)
    val distance = compile(shape.shape, local)
    return emit(SdfOps.SCALE, distances++, distance, 0, node)
  }

  private fun node(rows: FloatArray, values: FloatArray): Int {
    rows.forEach(nodes::add)
    for (index in 0 until VALUE_COUNT) nodes.add(values.getOrElse(index) { 0f })
    return nodes.size / SdfOps.NODE_FLOATS - 1
  }

  private fun emit(op: Int, target: Int, first: Int, second: Int, node: Int): Int {
    instructions.add(op)
    instructions.add(target)
    instructions.add(first)
    instructions.add(second)
    instructions.add(node)
    return target
  }

  /**
   * The rows of the affine map from world to [shape]'s child space: the inverse rotation divided by
   * the scale, then minus that applied to the translation.
   */
  private fun inverseRows(shape: Transformed): FloatArray {
    val inverse = rotationColumns(shape.rotation)
    val rows = FloatArray(SdfOps.VALUES)
    for (row in 0 until AXES) {
      val mx = inverse[row * AXES] / shape.scale
      val my = inverse[row * AXES + 1] / shape.scale
      val mz = inverse[row * AXES + 2] / shape.scale
      rows[row * ROW_FLOATS] = mx
      rows[row * ROW_FLOATS + 1] = my
      rows[row * ROW_FLOATS + 2] = mz
      rows[row * ROW_FLOATS + AXES] = -(mx * shape.x + my * shape.y + mz * shape.z)
    }
    return rows
  }

  internal companion object {
    private const val VALUE_COUNT = 4

    /** [shape] as a program. */
    fun compile(shape: SdfShape): SdfProgram {
      val compiler = SdfCompiler()
      val result = compiler.compile(shape, 0)
      return SdfProgram(
        compiler.instructions.toIntArray(),
        compiler.nodes.toFloatArray(),
        compiler.points,
        compiler.distances,
        result,
      )
    }

    /**
     * The columns of [rotation]'s matrix, which are the rows of its inverse, nine floats row by
     * row.
     */
    @Suppress("MagicNumber") // The quaternion-to-matrix formula's twos.
    private fun rotationColumns(rotation: Quat): FloatArray {
      val x = rotation.x()
      val y = rotation.y()
      val z = rotation.z()
      val w = rotation.w()
      return floatArrayOf(
        1f - 2f * (y * y + z * z),
        2f * (x * y + w * z),
        2f * (x * z - w * y),
        2f * (x * y - w * z),
        1f - 2f * (x * x + z * z),
        2f * (y * z + w * x),
        2f * (x * z + w * y),
        2f * (y * z - w * x),
        1f - 2f * (x * x + y * y),
      )
    }
  }
}
