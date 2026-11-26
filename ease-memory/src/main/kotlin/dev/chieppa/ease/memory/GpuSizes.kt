package dev.chieppa.ease.memory

/** Floats a 4x4 matrix takes, in arrays and in a WGSL `mat4x4f`. */
public const val MAT4_FLOATS: Int = 16

/** Bytes a 4x4 float matrix takes, in arrays and in a WGSL `mat4x4f`. */
public const val MAT4_BYTES: Long = MAT4_FLOATS * 4L
