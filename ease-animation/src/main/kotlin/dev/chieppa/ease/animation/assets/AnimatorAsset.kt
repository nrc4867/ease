package dev.chieppa.ease.animation.assets

import dev.chieppa.ease.annotations.Doc
import dev.chieppa.ease.assets.Guid
import kotlinx.serialization.Serializable

@Doc(
  "How a model animates: the parameters its states read, the states (a clip, or clips blended by " +
    "a parameter), the transitions between them, the sockets on its bones and the probes run on " +
    "its clip events. An Animator on a model's placement plays it."
)
@Serializable
public class AnimatorAsset(
  @Doc("The parameters transitions and blends read, by name.")
  public val parameters: List<AnimatorParameter> = emptyList(),
  @Doc("The states, by name; the first is where playback starts unless start names another.")
  public val states: List<AnimatorState> = emptyList(),
  @Doc("The transitions, tried in order; the first whose conditions all hold is taken.")
  public val transitions: List<AnimatorTransition> = emptyList(),
  @Doc("The state playback starts in; empty starts in the first state.")
  public val start: String = "",
  @Doc("Named places on bones that nodes and probes can follow.")
  public val sockets: List<AnimatorSocket> = emptyList(),
  @Doc("Surface probes on bones or sockets, each run when its clip event fires.")
  public val probes: List<AnimatorProbe> = emptyList(),
)

@Doc("A number states and transitions read, such as speed.")
@Serializable
public class AnimatorParameter(
  @Doc("The parameter's name.") public val name: String,
  @Doc("What sets it: the game's code, or the node's own movement.")
  public val drive: ParameterDrive = ParameterDrive.GAME,
  @Doc("Its value before anything sets it.") public val initial: Float = 0f,
)

@Doc("What sets an animator parameter.")
@Serializable
public enum class ParameterDrive {
  @Doc("The game's code sets it through the world's Animators.") GAME,
  @Doc(
    "The animated node's speed across the ground: how far its world position moved along x and z " +
      "in the last tick, in world units a second."
  )
  MOTION_SPEED,
}

@Doc(
  "One state: a single clip, or clips blended along a parameter. Every clip of a state plays at " +
    "the same normalized time, so blended walks keep their feet in step."
)
@Serializable
public class AnimatorState(
  @Doc("The state's name.") public val name: String,
  @Doc("The clip asset it plays; ignored when blend is set.") public val clip: Guid? = null,
  @Doc("Clips blended along a parameter, in place of a single clip.")
  public val blend: AnimatorBlend? = null,
  @Doc("Whether it starts again when it ends; otherwise it holds its last pose.")
  public val loop: Boolean = true,
  @Doc("How fast it plays: 1 is the clips' own speed.") public val speed: Float = 1f,
)

@Doc(
  "Clips placed along one parameter: each clip has full weight at its point, and between two " +
    "points the two nearest share the weight linearly. Past either end the end clip plays alone."
)
@Serializable
public class AnimatorBlend(
  @Doc("The parameter that picks the blend.") public val parameter: String,
  @Doc("The clips and where each sits along the parameter, in increasing order.")
  public val points: List<AnimatorBlendPoint>,
)

@Doc("One clip of a blend and where it sits along the blend's parameter.")
@Serializable
public class AnimatorBlendPoint(
  @Doc("The clip asset.") public val clip: Guid,
  @Doc("The parameter value at which it has full weight.") public val at: Float,
)

@Doc(
  "A move from one state to another when every condition holds, crossfading over duration. An " +
    "empty from leaves any state but the target."
)
@Serializable
public class AnimatorTransition(
  @Doc("The state it leaves; empty for any state.") public val from: String = "",
  @Doc("The state it enters.") public val to: String,
  @Doc("What must hold, all of it, for the transition to be taken.")
  public val conditions: List<AnimatorCondition> = emptyList(),
  @Doc("How long the crossfade takes, in seconds; 0 cuts.") public val duration: Float = 0.2f,
)

@Doc("A test of one parameter against a value.")
@Serializable
public class AnimatorCondition(
  @Doc("The parameter tested.") public val parameter: String,
  @Doc("How it is compared.") public val test: Comparison,
  @Doc("The value it is compared with.") public val value: Float,
)

@Doc("How a condition compares its parameter with its value.")
@Serializable
public enum class Comparison {
  @Doc("Holds when the parameter is greater than the value.") ABOVE,
  @Doc("Holds when the parameter is less than the value.") BELOW,
}

@Doc("A named place on a bone, offset in the bone's own space, that nodes and probes can follow.")
@Serializable
public class AnimatorSocket(
  @Doc("The socket's name.") public val name: String,
  @Doc("The bone it rides on, by node name.") public val bone: String,
  @Doc("Offset along the bone's x, in its units.") public val x: Float = 0f,
  @Doc("Offset along the bone's y, in its units.") public val y: Float = 0f,
  @Doc("Offset along the bone's z, in its units.") public val z: Float = 0f,
)

@Doc(
  "A point on a bone or socket that asks the physics world for the surface below it whenever its " +
    "clip event fires, such as a foot's probe on its footstep event. Each run is reported to the " +
    "world's AnimationEvents with the surface found."
)
@Serializable
public class AnimatorProbe(
  @Doc("The probe's name, such as leftFoot.") public val name: String,
  @Doc("The socket, or else the bone by node name, it sits on.") public val at: String,
  @Doc("The clip event that runs it.") public val event: String,
  @Doc("How far above the point the downward ray starts, in world units.")
  public val lift: Float = DEFAULT_LIFT,
  @Doc("How far below the point the ray still finds ground, in world units.")
  public val depth: Float = DEFAULT_DEPTH,
) {
  public companion object {
    /** How far above its point a probe's ray starts unless told otherwise, in world units. */
    public const val DEFAULT_LIFT: Float = 0.25f

    /** How far below its point a probe finds ground unless told otherwise, in world units. */
    public const val DEFAULT_DEPTH: Float = 0.5f
  }
}
