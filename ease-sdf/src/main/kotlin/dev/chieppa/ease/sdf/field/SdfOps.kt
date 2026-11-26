package dev.chieppa.ease.sdf.field

/**
 * The instructions of an [SdfProgram], each [WIDTH] ints: the opcode, the register written, two
 * registers read and the node whose parameters it reads. Primitives read a point register and write
 * a distance; [TRANSFORM] reads a point and writes one; [SCALE] and the joins read and write
 * distances.
 */
public object SdfOps {
  /** Primitives, reading their sizes from the node's values. */
  public const val SPHERE: Int = 0
  public const val BOX: Int = 1
  public const val ROUNDED_BOX: Int = 2
  public const val CAPSULE: Int = 3
  public const val CYLINDER: Int = 4
  public const val PLANE: Int = 5
  public const val TORUS: Int = 6

  /** Maps a point into a child's space by the node's affine rows. */
  public const val TRANSFORM: Int = 7

  /** Scales a child's distance back by the node's first value. */
  public const val SCALE: Int = 8

  /** Joins of two distances, blended by the node's first value. */
  public const val UNION: Int = 9
  public const val SUBTRACT: Int = 10
  public const val INTERSECT: Int = 11

  /** Ints one instruction takes, and where each sits in it. */
  internal const val WIDTH = 5
  internal const val OP = 0
  internal const val TARGET = 1
  internal const val FIRST = 2
  internal const val SECOND = 3
  internal const val NODE = 4

  /** Floats one node's parameters take: three affine rows, then four values. */
  public const val NODE_FLOATS: Int = 16

  /** Where a node's values start among its parameters. */
  public const val VALUES: Int = 12
}
