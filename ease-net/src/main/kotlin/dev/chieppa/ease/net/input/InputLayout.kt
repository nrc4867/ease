package dev.chieppa.ease.net.input

/**
 * The shape of one tick of a game's input: [axes] analog values from -1 to 1, such as a stick's x
 * and y, and [buttons] that are pressed or not. Host and clients of a session use the same layout.
 */
public class InputLayout(public val axes: Int, public val buttons: Int) {
  init {
    require(axes in 0..MAX_AXES) { "An input has 0 to $MAX_AXES axes, not $axes" }
    require(buttons in 0..MAX_BUTTONS) { "An input has 0 to $MAX_BUTTONS buttons, not $buttons" }
  }

  /** The bits one tick of input takes on the wire. */
  public val frameBits: Int
    get() = axes * AXIS_BITS + buttons

  public companion object {
    /** The most axes an input has. */
    public const val MAX_AXES: Int = 16

    /** The most buttons an input has: they are one `Int` of bits. */
    public const val MAX_BUTTONS: Int = 32

    /** Bits one axis takes: steps of 1/32767 from -1 to 1, so 0 and both ends are exact. */
    public const val AXIS_BITS: Int = 16
  }
}
