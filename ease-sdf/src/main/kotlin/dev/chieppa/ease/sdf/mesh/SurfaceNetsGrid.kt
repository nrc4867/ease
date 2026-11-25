package dev.chieppa.ease.sdf.mesh

import dev.chieppa.ease.memory.GpuStruct

/**
 * An [SdfGrid] as the surface nets kernels read it: its cell size and cells along each axis, how
 * many corners it has, and how many workgroups of 256 corners cover them. Corner coordinates come
 * from a table the CPU fills, so both sides place corners on the same bits.
 */
@GpuStruct
public class SurfaceNetsGrid(
  public val cellSize: Float,
  public val cellsX: UInt,
  public val cellsY: UInt,
  public val cellsZ: UInt,
  public val points: UInt,
  public val blocks: UInt,
)
