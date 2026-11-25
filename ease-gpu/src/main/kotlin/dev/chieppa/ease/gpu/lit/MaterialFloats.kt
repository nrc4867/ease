package dev.chieppa.ease.gpu.lit

/**
 * How a lit material is packed into floats wherever it is kept in arrays: base color red, green and
 * blue, roughness, metallic, then emitted red, green and blue.
 */
public object MaterialFloats {
  /** Floats one material takes. */
  public const val COUNT: Int = 8

  /** Where the base color starts. */
  public const val BASE_COLOR: Int = 0

  /** Where roughness is. */
  public const val ROUGHNESS: Int = 3

  /** Where metallic is. */
  public const val METALLIC: Int = 4

  /** Where the emitted color starts. */
  public const val EMISSIVE: Int = 5
}
