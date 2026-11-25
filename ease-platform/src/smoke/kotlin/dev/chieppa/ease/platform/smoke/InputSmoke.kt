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
import dev.chieppa.ease.platform.time.SystemClock
import java.util.Locale
import kotlin.math.abs

/** How far the camera moves between two printed positions. */
private const val PRINT_STEP = 0.25f

private const val NANOS_PER_SECOND = 1_000_000_000f

/** Camera units per second at full push. */
private const val SPEED = 3f

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
  var camera = Pair(0f, 0f)
  var printed = camera
  var shown = input.activeDevice
  var last = SystemClock.nanoTime()
  println("Move with WASD / arrows or a gamepad's left stick / d-pad; quit with escape or start.")
  while (true) {
    val closing = windowing.pollEvents().any { it.second == WindowEvent.CloseRequested }
    input.update()
    if (closing || input.pressed(quit)) return
    val now = SystemClock.nanoTime()
    val seconds = (now - last) / NANOS_PER_SECOND
    last = now
    val dx = input.value(moveX)
    val dy = input.value(moveY)
    if (dx != 0f || dy != 0f) {
      camera = Pair(camera.first + dx * SPEED * seconds, camera.second + dy * SPEED * seconds)
      if (abs(camera.first - printed.first) + abs(camera.second - printed.second) >= PRINT_STEP) {
        printed = camera
        println(String.format(Locale.ROOT, "camera %.2f, %.2f", camera.first, camera.second))
      }
    } else {
      // Nothing is held: sleep inside SDL until a key, stick or window event arrives.
      windowing.awaitEvent()
      last = SystemClock.nanoTime()
    }
    if (input.activeDevice != shown) {
      shown = input.activeDevice
      println("Using $shown; quit is ${input.glyph(quit)}")
    }
  }
}
