package dev.chieppa.ease.platform.input

/**
 * Ties [control] (such as `key.space` or `gamepad.left_x`) to the action called [action]. An axis
 * action reads the control's value times [scale], so `key.a` with scale -1 pushes it left; a button
 * action is pressed while that product is at least [PRESS_THRESHOLD].
 */
public data class Binding(val action: String, val control: String, val scale: Float = 1f)

/**
 * A set of bindings that is active while on the [InputContextStack]: gameplay, a menu, a dialogue.
 * An action bound in a context overrides the same action bound in contexts below it; with
 * [blocksBelow] no action of the contexts below reads at all.
 */
public class InputContext(public val name: String, public val blocksBelow: Boolean = false) {
  private val entries = mutableListOf<Binding>()

  /** The bindings, in the order they were added. */
  public val bindings: List<Binding>
    get() = entries

  /** Adds a binding of [control] to [action]. */
  public fun bind(action: String, control: String, scale: Float = 1f): InputContext = apply {
    entries += Binding(action, control, scale)
  }

  /**
   * Makes [control] the way to trigger [action] on the device family it belongs to: bindings of
   * [action] on that family are replaced, those on other families stay. The new binding keeps the
   * scale of the first one it replaces, or [scale] when there was none.
   */
  public fun rebind(action: String, control: String, scale: Float = 1f) {
    val kind = requireNotNull(DeviceKind.of(control)) { "Unknown control $control" }
    val replaced = entries.firstOrNull { it.action == action && DeviceKind.of(it.control) == kind }
    entries.removeAll { it.action == action && DeviceKind.of(it.control) == kind }
    entries += Binding(action, control, replaced?.scale ?: scale)
  }

  /** Replaces every binding with [replacement]. */
  public fun replaceBindings(replacement: List<Binding>) {
    entries.clear()
    entries += replacement
  }
}

/** The contexts in force, the last pushed on top. */
public class InputContextStack {
  private val stack = mutableListOf<InputContext>()

  /** How many contexts are in force. */
  public val size: Int
    get() = stack.size

  /** The context at [index], 0 being the bottom. */
  public operator fun get(index: Int): InputContext = stack[index]

  /** Puts [context] on top. */
  public fun push(context: InputContext) {
    stack += context
  }

  /** Takes the top context off and returns it. */
  public fun pop(): InputContext = stack.removeAt(stack.lastIndex)

  /** The contexts, bottom first. */
  public val contexts: List<InputContext>
    get() = stack
}
