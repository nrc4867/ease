package dev.chieppa.ease.platform.input

/** What an action reads as: a press or a value between -1 and 1. */
public enum class ActionKind {
  /** Pressed or not; reads 0 or 1. */
  BUTTON,

  /** An analog value from -1 to 1, such as one direction of a stick. */
  AXIS,
}

/** An action a game understands, such as `jump` or `move_x`; devices never reach game code. */
public data class ActionDefinition(val name: String, val kind: ActionKind)

/** The actions of a game; an action's number is its position in [actions]. */
public class ActionSet(public val actions: List<ActionDefinition>) {
  private val indices: Map<String, Int> =
    actions.withIndex().associate { it.value.name to it.index }

  init {
    require(indices.size == actions.size) { "Action names must be unique" }
  }

  /** The number of the action called [name], or -1 when there is none. */
  public fun indexOf(name: String): Int = indices[name] ?: -1

  /** The number of the action called [name]; fails when there is none. */
  public fun indexOfOrThrow(name: String): Int =
    indexOf(name).also { require(it >= 0) { "No action called $name" } }
}
