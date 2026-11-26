package dev.chieppa.ease.sdf.field

/**
 * A shape tree flattened into straight-line instructions over registers, the one form both the
 * Kotlin evaluator and generated WGSL run. Each instruction has an [op] from [SdfOps], writes
 * register [target], reads registers [first] and [second], and takes its parameters from [node]:
 * [SdfOps.NODE_FLOATS] floats, three affine rows then four values. Point register 0 is the point
 * asked about; the distance ends in register [result]. Children come before parents and every
 * result gets a new register, so the same tree always gives the same program.
 */
public class SdfProgram
internal constructor(
  private val instructions: IntArray,
  private val nodes: FloatArray,
  /** How many point registers the program uses. */
  public val pointRegisters: Int,
  /** How many distance registers the program uses. */
  public val distanceRegisters: Int,
  /** The distance register holding the answer. */
  public val result: Int,
) {
  /** How many instructions there are. */
  public val size: Int
    get() = instructions.size / SdfOps.WIDTH

  /** How many nodes hold parameters. */
  public val nodeCount: Int
    get() = nodes.size / SdfOps.NODE_FLOATS

  /** The opcode of [instruction]. */
  public fun op(instruction: Int): Int = at(instruction, SdfOps.OP)

  /** The register [instruction] writes. */
  public fun target(instruction: Int): Int = at(instruction, SdfOps.TARGET)

  /** The first register [instruction] reads. */
  public fun first(instruction: Int): Int = at(instruction, SdfOps.FIRST)

  /** The second register [instruction] reads, for joins. */
  public fun second(instruction: Int): Int = at(instruction, SdfOps.SECOND)

  /** The node whose parameters [instruction] reads. */
  public fun node(instruction: Int): Int = at(instruction, SdfOps.NODE)

  /** Parameter [index] (0 to 15) of [node]. */
  public fun parameter(node: Int, index: Int): Float = nodes[node * SdfOps.NODE_FLOATS + index]

  private fun at(instruction: Int, slot: Int): Int = instructions[instruction * SdfOps.WIDTH + slot]
}
