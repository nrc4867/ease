package dev.chieppa.ease.sdf.field

import dev.chieppa.ease.memory.NativeWriter
import dev.chieppa.ease.sdf.shape.SdfShape
import java.lang.foreign.MemorySegment

/**
 * A [shape] compiled once into the program both sides run: [distance] evaluates it in Kotlin, and
 * [wgsl] is the same program as a WGSL function, `sdf_field(p: vec3f) -> f32`, reading its
 * parameters from `sdf_nodes`, an `array<SdfNode>` a shader binds and fills with [writeNodes].
 * Evaluating allocates nothing but reuses registers, so use one field from one thread at a time.
 */
public class DistanceField(public val shape: SdfShape) {
  internal val program: SdfProgram = SdfCompiler.compile(shape)
  private val points = FloatArray(program.pointRegisters * AXES)
  private val distances = FloatArray(program.distanceRegisters)

  /** The WGSL of `sdf_field`, after the distance functions it calls. */
  public val wgsl: String = SdfWgsl.field(program)

  /** How many [SdfNode]s [writeNodes] writes. */
  public val nodeCount: Int
    get() = program.nodeCount

  /** Bytes [writeNodes] writes. */
  public val nodeBytes: Long
    get() = nodeCount * SdfNodeWriter.STRUCT_SIZE

  /** The signed distance from ([x], [y], [z]) to the shape's surface: negative inside. */
  public fun distance(x: Float, y: Float, z: Float): Float {
    points[0] = x
    points[1] = y
    points[2] = z
    for (instruction in 0 until program.size) run(instruction)
    return distances[program.result]
  }

  /** Writes every node's parameters into [segment], from its start, as `array<SdfNode>`. */
  public fun writeNodes(segment: MemorySegment) {
    require(segment.byteSize() >= nodeBytes) {
      "${segment.byteSize()} bytes cannot hold $nodeCount nodes"
    }
    val writer = NativeWriter().begin(segment)
    val rows =
      listOf(
        SdfNodeWriter::inverse0,
        SdfNodeWriter::inverse1,
        SdfNodeWriter::inverse2,
        SdfNodeWriter::values,
      )
    for (node in 0 until nodeCount) {
      rows.forEachIndexed { row, write ->
        val at = row * ROW
        write(
          writer,
          program.parameter(node, at),
          program.parameter(node, at + 1),
          program.parameter(node, at + 2),
          program.parameter(node, at + AXES),
        )
      }
      writer.next(SdfNodeWriter.STRUCT_SIZE)
    }
  }

  private fun run(instruction: Int) {
    val target = program.target(instruction)
    val first = program.first(instruction)
    val second = program.second(instruction)
    val node = program.node(instruction)
    when (val op = program.op(instruction)) {
      SdfOps.TRANSFORM -> transform(target, first, node)
      SdfOps.SCALE -> distances[target] = distances[first] * value(node, 0)
      SdfOps.UNION ->
        distances[target] = SdfMath.union(distances[first], distances[second], value(node, 0))
      SdfOps.SUBTRACT ->
        distances[target] = SdfMath.subtract(distances[first], distances[second], value(node, 0))
      SdfOps.INTERSECT ->
        distances[target] = SdfMath.intersect(distances[first], distances[second], value(node, 0))
      else -> distances[target] = primitive(op, first, node)
    }
  }

  @Suppress("CyclomaticComplexMethod") // One branch per primitive.
  private fun primitive(op: Int, point: Int, node: Int): Float {
    val x = points[point * AXES]
    val y = points[point * AXES + 1]
    val z = points[point * AXES + 2]
    val a = value(node, 0)
    val b = value(node, 1)
    val c = value(node, 2)
    val d = value(node, AXES)
    return when (op) {
      SdfOps.SPHERE -> SdfMath.sphere(x, y, z, a)
      SdfOps.BOX -> SdfMath.box(x, y, z, a, b, c)
      SdfOps.ROUNDED_BOX -> SdfMath.roundedBox(x, y, z, a, b, c, d)
      SdfOps.CAPSULE -> SdfMath.capsule(x, y, z, a, b)
      SdfOps.CYLINDER -> SdfMath.cylinder(x, y, z, a, b)
      SdfOps.PLANE -> SdfMath.plane(x, y, z, a, b, c, d)
      SdfOps.TORUS -> SdfMath.torus(x, y, z, a, b)
      else -> error("Opcode $op is not a primitive")
    }
  }

  private fun transform(target: Int, source: Int, node: Int) {
    val x = points[source * AXES]
    val y = points[source * AXES + 1]
    val z = points[source * AXES + 2]
    for (row in 0 until AXES) {
      val at = row * ROW
      val mx = program.parameter(node, at)
      val my = program.parameter(node, at + 1)
      val mz = program.parameter(node, at + 2)
      val offset = program.parameter(node, at + AXES)
      points[target * AXES + row] = mx * x + my * y + mz * z + offset
    }
  }

  private fun value(node: Int, index: Int): Float = program.parameter(node, SdfOps.VALUES + index)

  private companion object {
    const val AXES = 3
    const val ROW = 4
  }
}
