package dev.chieppa.ease.sdf

/** Sizes the distance-field code shares, on the CPU and in what it hands the GPU. */
public object SdfConstants {
  /** Coordinates of a point or vector, and corners of a triangle. */
  public const val AXES: Int = 3

  /** Floats in one row of an affine map: three factors and an offset. */
  public const val ROW_FLOATS: Int = 4
}
