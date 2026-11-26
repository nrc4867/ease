package dev.chieppa.ease.sdf.shape

/** How [Combined] joins its two shapes. */
public enum class CsgOp {
  /** Everything in either shape. */
  UNION,

  /** The first shape with the second carved out of it. */
  SUBTRACT,

  /** Only what is in both shapes. */
  INTERSECT,
}
