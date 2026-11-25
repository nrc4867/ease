package dev.chieppa.ease.sample

import dev.chieppa.ease.net.input.InputFrame
import dev.chieppa.ease.runtime.ClientWorld
import dev.chieppa.ease.runtime.InputScript

/**
 * The sample's scripted input, run on a client: it holds the stick in one direction and turns it a
 * quarter left every [TURN_EVERY] ticks, so the client's courier (see [SampleGame.peerJoined])
 * traces a square loop, moved only by the client's input and predicted on the client.
 */
public class CourierScript : InputScript {
  override fun send(client: ClientWorld) {
    steer(client.inputFrame, client.tick)
  }

  public companion object {
    /** Ticks between two turns: half a second at the default rate. */
    public const val TURN_EVERY: Long = 30

    private const val TURNS = 4

    /** Right, up, left, down: the stick turns left a quarter at each turn. */
    private val DIRECTION_X = floatArrayOf(1f, 0f, -1f, 0f)
    private val DIRECTION_Y = floatArrayOf(0f, 1f, 0f, -1f)

    /** Sets [frame] to the script's input for [tick]. */
    public fun steer(frame: InputFrame, tick: Long) {
      val turn = (tick / TURN_EVERY % TURNS).toInt()
      frame.setAxis(CourierSystem.AXIS_X, DIRECTION_X[turn])
      frame.setAxis(CourierSystem.AXIS_Y, DIRECTION_Y[turn])
    }

    /** The courier as it spawns: white, at the center, standing still. */
    public val COURIER: Cube =
      Cube(
        x = 0f,
        y = 0f,
        speedX = 0f,
        speedY = 0f,
        size = 0.7f,
        red = 0.9f,
        green = 0.9f,
        blue = 0.9f,
      )
  }
}
