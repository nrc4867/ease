package dev.chieppa.ease.sdf.field

/**
 * The instructions of an [SdfProgram], each five ints: the opcode, the register written, two
 * registers read and the node whose parameters it reads. Primitives read a point register and write
 * a distance; [TRANSFORM] reads a point and writes one; [SCALE] and the joins read and write
 * distances.
 */
internal object SdfOps {
  const val SPHERE = 0
  const val BOX = 1
  const val ROUNDED_BOX = 2
  const val CAPSULE = 3
  const val CYLINDER = 4
  const val PLANE = 5
  const val TORUS = 6
  const val TRANSFORM = 7
  const val SCALE = 8
  const val UNION = 9
  const val SUBTRACT = 10
  const val INTERSECT = 11

  /** Ints one instruction takes, and where each sits in it. */
  const val WIDTH = 5
  const val OP = 0
  const val TARGET = 1
  const val FIRST = 2
  const val SECOND = 3
  const val NODE = 4

  /** Floats one node's parameters take: three rows of an affine map, then four values. */
  const val NODE_FLOATS = 16
  const val VALUES = 12
}
