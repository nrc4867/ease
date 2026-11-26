package dev.chieppa.ease.scene.transform

/** Floats of one world transform: position x, y, z, rotation x, y, z, w, scale x, y, z. */
public const val WORLD_TRANSFORM_FLOATS: Int = 10

/** Where a world transform's rotation (x, y, z, w) starts among its floats. */
public const val TRANSFORM_ROTATION_AT: Int = 3

/** Where a world transform's rotation w sits among its floats. */
public const val TRANSFORM_ROTATION_W: Int = 6

/** Where a world transform's scale (x, y, z) starts among its floats. */
public const val TRANSFORM_SCALE_AT: Int = 7
