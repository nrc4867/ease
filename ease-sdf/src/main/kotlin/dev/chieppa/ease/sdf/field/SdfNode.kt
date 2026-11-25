package dev.chieppa.ease.sdf.field

import dev.chieppa.ease.math.Vec4
import dev.chieppa.ease.memory.GpuStruct

/**
 * One node's parameters, as generated WGSL reads them from its node array. A transform maps a point
 * into its child's space by the affine rows [inverse0], [inverse1] and [inverse2] (x, y and z of
 * the result: a row's xyz dotted with the point, plus its w) and scales the child's distance back
 * by [values]' x. A primitive reads its sizes from [values]; a join reads its blend radius from its
 * x.
 */
@GpuStruct
public class SdfNode(
  public val inverse0: Vec4,
  public val inverse1: Vec4,
  public val inverse2: Vec4,
  public val values: Vec4,
)
