package dev.chieppa.ease.platform.smoke

import dev.chieppa.ease.platform.WindowEvent
import dev.chieppa.ease.platform.WindowSpec
import dev.chieppa.ease.platform.input.ActionDefinition
import dev.chieppa.ease.platform.input.ActionInput
import dev.chieppa.ease.platform.input.ActionKind
import dev.chieppa.ease.platform.input.ActionSet
import dev.chieppa.ease.platform.input.InputContext
import dev.chieppa.ease.platform.sdl.SdlInput
import dev.chieppa.ease.platform.sdl.SdlWindowing
import java.util.Locale
import java.util.concurrent.locks.LockSupport

private const val FRAME_NANOS = 16_000_000L
private const val SPEED = 0.05f

/**
 * A camera moved by actions: WASD or the arrow keys, and a gamepad's left stick or d-pad, move it;
 * escape or the gamepad's start button quits. The camera's position prints when it changes, and the
 * glyph ids for "quit" print when the device in use changes.
 */
public fun main() {
  val actions =
    ActionSet(
      listOf(
        ActionDefinition("move_x", ActionKind.AXIS),
        ActionDefinition("move_y", ActionKind.AXIS),
        ActionDefinition("quit", ActionKind.BUTTON),
      )
    )
  val gameplay =
    InputContext("gameplay")
      .bind("move_x", "key.d")
      .bind("move_x", "key.a", -1f)
      .bind("move_x", "key.right")
      .bind("move_x", "key.left", -1f)
      .bind("move_y", "key.w")
      .bind("move_y", "key.s", -1f)
      .bind("move_y", "key.up")
      .bind("move_y", "key.down", -1f)
      .bind("move_x", "gamepad.left_x")
      .bind("move_y", "gamepad.left_y")
      .bind("move_x", "gamepad.dpad_right")
      .bind("move_x", "gamepad.dpad_left", -1f)
      .bind("move_y", "gamepad.dpad_up")
      .bind("move_y", "gamepad.dpad_down", -1f)
      .bind("quit", "key.escape")
      .bind("quit", "gamepad.start")
  SdlWindowing().use { windowing ->
    windowing.open(WindowSpec("Ease input smoke", 640, 360))
    SdlInput.open().use { devices ->
      val input = ActionInput(devices, actions)
      input.contexts.push(gameplay)
      run(windowing, input)
    }
  }
  println("Closed cleanly")
}

private fun run(windowing: SdlWindowing, input: ActionInput) {
  val moveX = input.actions.indexOf("move_x")
  val moveY = input.actions.indexOf("move_y")
  val quit = input.actions.indexOf("quit")
  var x = 0f
  var y = 0f
  var shown = input.activeDevice
  println("Move with WASD / arrows or a gamepad's left stick / d-pad; quit with escape or start.")
  while (true) {
    val closing = windowing.pollEvents().any { it.second == WindowEvent.CloseRequested }
    input.update()
    if (closing || input.pressed(quit)) return
    val dx = input.value(moveX)
    val dy = input.value(moveY)
    x += dx * SPEED
    y += dy * SPEED
    if (dx != 0f || dy != 0f) println(String.format(Locale.ROOT, "camera %.2f, %.2f", x, y))
    if (input.activeDevice != shown) {
      shown = input.activeDevice
      println("Using $shown; quit is ${input.glyph(quit)}")
    }
    LockSupport.parkNanos(FRAME_NANOS)
  }
}
