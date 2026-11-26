package dev.chieppa.ease.render.assets.animation

private const val VEC3 = 3
private const val QUAT = 4

/** Which node property a clip channel drives. */
public enum class ChannelPath(
  /** Floats one keyframe value of the property takes; 0 for morph weights, which vary. */
  public val floats: Int
) {
  /** Position, three floats. */
  TRANSLATION(VEC3),

  /** Rotation quaternion x, y, z, w. */
  ROTATION(QUAT),

  /** Scale, three floats. */
  SCALE(VEC3),

  /** Morph target weights, one float a target. */
  WEIGHTS(0),
}

/** How a channel blends between keyframes. */
public enum class Interpolation {
  /** Each value holds until the next keyframe. */
  STEP,

  /** Straight lines between keyframes; rotations along the shortest arc. */
  LINEAR,

  /** Cubic Hermite splines: each keyframe holds an in-tangent, a value and an out-tangent. */
  CUBICSPLINE,
}

/**
 * One animated property: the node it drives (the local id, 16 hex digits, of its node in the
 * model's prefab), the [path], keyframe [times] in seconds and their [values], packed keyframe
 * after keyframe (three entries a keyframe for [Interpolation.CUBICSPLINE]).
 */
public class ClipChannel(
  public val node: String,
  public val path: ChannelPath,
  public val interpolation: Interpolation,
  public val times: FloatArray,
  public val values: FloatArray,
)

/** A cooked animation clip: its [name], [duration] in seconds and [channels]. */
public class ClipAsset(
  public val name: String,
  public val duration: Float,
  public val channels: List<ClipChannel>,
)
