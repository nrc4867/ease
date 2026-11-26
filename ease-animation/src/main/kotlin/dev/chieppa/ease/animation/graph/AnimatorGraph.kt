package dev.chieppa.ease.animation.graph

import dev.chieppa.ease.animation.assets.AnimatorAsset
import dev.chieppa.ease.animation.assets.AnimatorState
import dev.chieppa.ease.animation.assets.ClipAsset
import dev.chieppa.ease.animation.assets.Comparison
import dev.chieppa.ease.animation.assets.ParameterDrive
import dev.chieppa.ease.assets.Guid
import dev.chieppa.ease.scene.node.LocalId

/**
 * An animator asset made ready to play: names resolved to indices and clips to the joints they
 * drive. [joints] lists, by prefab local id, every node any clip drives, in first-use order; a pose
 * holds one transform per joint. Made once per asset version; playing it allocates nothing.
 */
internal class AnimatorGraph(
  val asset: AnimatorAsset,
  val joints: Array<LocalId>,
  val clips: Array<GraphClip>,
  val states: Array<GraphState>,
  val transitions: Array<GraphTransition>,
  val start: Int,
) {
  /** The parameters' names, by index. */
  val parameterNames: Array<String> = Array(asset.parameters.size) { asset.parameters[it].name }

  /** Which parameters follow the node's own movement. */
  val motionDriven: BooleanArray =
    BooleanArray(asset.parameters.size) {
      asset.parameters[it].drive == ParameterDrive.MOTION_SPEED
    }

  /** The index of the parameter [name], or -1. */
  fun parameterIndex(name: String): Int = parameterNames.indexOf(name)

  /** The index of the state [name], or -1. */
  fun stateIndex(name: String): Int = states.indexOfFirst { it.name == name }

  companion object {
    /**
     * Compiles [asset], loading its clips through [clip]. Throws [IllegalArgumentException] naming
     * what is wrong: no states, a clip that does not load, an unknown state or parameter, blend
     * points out of order, a negative speed.
     */
    fun compile(asset: AnimatorAsset, clip: (Guid) -> ClipAsset?): AnimatorGraph =
      Compiler(asset, clip).run()
  }

  private class Compiler(val asset: AnimatorAsset, val load: (Guid) -> ClipAsset?) {
    val joints = LinkedHashMap<LocalId, Int>()
    val clips = mutableListOf<GraphClip>()
    val clipIndex = HashMap<Guid, Int>()
    val parameters = asset.parameters.map { it.name }

    fun run(): AnimatorGraph {
      require(asset.states.isNotEmpty()) { "An animator needs at least one state" }
      val names = asset.states.map { it.name }
      require(names.toSet().size == names.size) { "A state name is used twice in $names" }
      require(parameters.toSet().size == parameters.size) { "A parameter is named twice" }
      val states = asset.states.map(::state).toTypedArray()
      val transitions =
        asset.transitions
          .map { transition ->
            val from = if (transition.from.isEmpty()) -1 else stateOf(names, transition.from)
            val conditions = transition.conditions
            GraphTransition(
              from,
              stateOf(names, transition.to),
              IntArray(conditions.size) { parameterOf(conditions[it].parameter) },
              BooleanArray(conditions.size) { conditions[it].test == Comparison.ABOVE },
              FloatArray(conditions.size) { conditions[it].value },
              maxOf(transition.duration, 0f),
            )
          }
          .toTypedArray()
      val start = if (asset.start.isEmpty()) 0 else stateOf(names, asset.start)
      return AnimatorGraph(
        asset,
        joints.keys.toTypedArray(),
        clips.toTypedArray(),
        states,
        transitions,
        start,
      )
    }

    private fun state(state: AnimatorState): GraphState {
      require(state.speed >= 0f) {
        "State ${state.name} plays at speed ${state.speed}; a state cannot play backwards"
      }
      val blend = state.blend
      if (blend == null) {
        val guid = requireNotNull(state.clip) { "State ${state.name} names no clip and no blend" }
        return GraphState(
          state.name,
          intArrayOf(clip(guid)),
          FloatArray(1),
          -1,
          state.loop,
          state.speed,
        )
      }
      require(blend.points.isNotEmpty()) { "State ${state.name}'s blend has no points" }
      val points = FloatArray(blend.points.size) { blend.points[it].at }
      for (index in 1 until points.size) {
        require(points[index] > points[index - 1]) {
          "State ${state.name}'s blend points must increase, not ${points.toList()}"
        }
      }
      return GraphState(
        state.name,
        IntArray(blend.points.size) { clip(blend.points[it].clip) },
        points,
        parameterOf(blend.parameter),
        state.loop,
        state.speed,
      )
    }

    private fun clip(guid: Guid): Int =
      clipIndex.getOrPut(guid) {
        val asset = requireNotNull(load(guid)) { "Clip $guid does not load" }
        val slots =
          IntArray(asset.channels.size) {
            val id = LocalId.parse(asset.channels[it].node)
            joints.getOrPut(id) { joints.size }
          }
        clips += GraphClip(guid, asset, slots)
        clips.size - 1
      }

    private fun stateOf(names: List<String>, name: String): Int {
      val index = names.indexOf(name)
      require(index >= 0) { "No state is named $name; the states are $names" }
      return index
    }

    private fun parameterOf(name: String): Int {
      val index = parameters.indexOf(name)
      require(index >= 0) { "No parameter is named $name; the parameters are $parameters" }
      return index
    }
  }
}
