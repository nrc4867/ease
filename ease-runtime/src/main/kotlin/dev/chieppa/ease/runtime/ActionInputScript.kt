package dev.chieppa.ease.runtime

import dev.chieppa.ease.platform.input.ActionInput
import dev.chieppa.ease.platform.input.ActionKind

/**
 * Feeds a client's per-tick input from the action layer: before each tick it updates [input] and
 * writes the actions into the client's [ClientWorld.inputFrame], action `axes[i]` into axis `i` and
 * action `buttons[i]` into button `i`. Devices, bindings and contexts stop here: the frame is all
 * that is predicted, sent and replayed, so a recorded or scripted device gives the same run.
 */
public class ActionInputScript(
  private val input: ActionInput,
  axes: List<String>,
  buttons: List<String>,
) : InputScript {
  private val axisActions = IntArray(axes.size) { checkKind(axes[it], ActionKind.AXIS) }
  private val buttonActions = IntArray(buttons.size) { checkKind(buttons[it], ActionKind.BUTTON) }

  override fun send(client: ClientWorld) {
    input.update()
    val frame = client.inputFrame
    for (axis in axisActions.indices) frame.setAxis(axis, input.value(axisActions[axis]))
    for (button in buttonActions.indices) {
      frame.setButton(button, input.pressed(buttonActions[button]))
    }
  }

  private fun checkKind(name: String, kind: ActionKind): Int {
    val index = input.actions.indexOfOrThrow(name)
    require(input.actions.actions[index].kind == kind) { "Action $name is not a $kind" }
    return index
  }
}
