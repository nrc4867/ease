package dev.chieppa.ease.net.input

import kotlin.math.roundToInt

/** The step that stands for an axis at 0; steps run from 0 (-1) to twice this (1). */
private const val AXIS_ZERO = 32767

/**
 * One tick of input in a [layout]: axes clamped to -1 to 1 and kept at the precision the wire
 * carries, so the host simulates exactly what the client did, and buttons as bits. Frames are
 * reused, never allocated per tick.
 */
public class InputFrame(public val layout: InputLayout) {
  private val axisSteps = IntArray(layout.axes) { AXIS_ZERO }

  /** The pressed buttons, bit `i` for button `i`. */
  public var buttons: Int = 0
    private set

  /** Axis [index], from -1 to 1. */
  public fun axis(index: Int): Float = (axisSteps[index] - AXIS_ZERO).toFloat() / AXIS_ZERO

  /** Sets axis [index] to [value], clamped to -1 to 1; a value that is not a number counts as 0. */
  public fun setAxis(index: Int, value: Float) {
    val clamped = if (value.isNaN()) 0f else value.coerceIn(-1f, 1f)
    axisSteps[index] = (clamped * AXIS_ZERO).roundToInt() + AXIS_ZERO
  }

  /** Whether button [index] is pressed. */
  public fun button(index: Int): Boolean = (buttons ushr index) and 1 != 0

  /** Presses or releases button [index]. */
  public fun setButton(index: Int, pressed: Boolean) {
    require(index in 0 until layout.buttons) { "Button $index is outside ${layout.buttons}" }
    buttons = if (pressed) buttons or (1 shl index) else buttons and (1 shl index).inv()
  }

  /** Makes this frame hold what [other] holds. */
  public fun copyFrom(other: InputFrame) {
    other.axisSteps.copyInto(axisSteps)
    buttons = other.buttons
  }

  /** Releases every button and centres every axis. */
  public fun clear() {
    axisSteps.fill(AXIS_ZERO)
    buttons = 0
  }

  internal fun axisStep(index: Int): Int = axisSteps[index]

  internal fun setAxisStep(index: Int, step: Int) {
    axisSteps[index] = step
  }

  internal fun setButtonBits(bits: Int) {
    buttons = bits
  }

  internal companion object {
    /** The highest step an axis holds; a received step above it is refused. */
    const val MAX_STEP: Int = 2 * AXIS_ZERO
  }
}
