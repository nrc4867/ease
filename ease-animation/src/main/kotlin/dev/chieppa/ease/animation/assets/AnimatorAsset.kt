package dev.chieppa.ease.animation.assets

import dev.chieppa.ease.annotations.Doc
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
