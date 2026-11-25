package dev.chieppa.ease.gpu.lit

import dev.chieppa.ease.math.Mat4
import dev.chieppa.ease.math.Vec3
import dev.chieppa.ease.memory.GpuStruct

/** The sun: the direction its light travels, how strong it is, and its linear color. */
@GpuStruct
public class DirectionalLightData(
  public val direction: Vec3,
  public val intensity: Float,
  public val color: Vec3,
)

/** What every draw of a frame shares: group 0, binding 0. */
@GpuStruct public class FrameData(public val light: DirectionalLightData, public val ambient: Vec3)

/** The camera a pass draws through: group 1, binding 0. */
@GpuStruct public class ViewData(public val viewProjection: Mat4, public val cameraPosition: Vec3)

/** One lit material's parameters, linear colors: group 2, binding 0, one per draw. */
@GpuStruct
public class LitMaterial(
  public val baseColor: Vec3,
  public val roughness: Float,
  public val emissive: Vec3,
  public val metallic: Float,
)

/**
 * One draw's transforms: group 3, binding 0, read by instance index. [normalMatrix] turns normals
 * to world space: the rotation with the scale inverted.
 */
@GpuStruct public class ObjectData(public val model: Mat4, public val normalMatrix: Mat4)
