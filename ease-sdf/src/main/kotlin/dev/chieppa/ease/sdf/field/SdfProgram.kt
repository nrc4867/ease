package dev.chieppa.ease.sdf.field

/**
 * A shape tree flattened into straight-line instructions over registers, the one form both the
 * Kotlin evaluator and the generated WGSL run: [instructions] as [SdfOps] lays them out, [nodes]
 * holding each node's [SdfOps.NODE_FLOATS] parameters in [SdfNode]'s layout, point register 0 as
 * the point asked about, and the distance in register [result].
 */
internal class SdfProgram(
  val instructions: IntArray,
  val nodes: FloatArray,
  val pointRegisters: Int,
  val distanceRegisters: Int,
  val result: Int,
) {
  /** How many instructions there are. */
  val size: Int
    get() = instructions.size / SdfOps.WIDTH

  /** How many nodes hold parameters. */
  val nodeCount: Int
    get() = nodes.size / SdfOps.NODE_FLOATS

  fun op(instruction: Int): Int = instructions[instruction * SdfOps.WIDTH + SdfOps.OP]

  fun target(instruction: Int): Int = instructions[instruction * SdfOps.WIDTH + SdfOps.TARGET]

  fun first(instruction: Int): Int = instructions[instruction * SdfOps.WIDTH + SdfOps.FIRST]

  fun second(instruction: Int): Int = instructions[instruction * SdfOps.WIDTH + SdfOps.SECOND]

  fun node(instruction: Int): Int = instructions[instruction * SdfOps.WIDTH + SdfOps.NODE]

  /** Parameter [index] (0 to 15) of [node]. */
  fun parameter(node: Int, index: Int): Float = nodes[node * SdfOps.NODE_FLOATS + index]
}
