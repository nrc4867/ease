package dev.chieppa.ease.sample

import com.github.quillraven.fleks.Family
import com.github.quillraven.fleks.World.Companion.family
import dev.chieppa.ease.ecs.replication.NetworkId
import dev.chieppa.ease.ecs.replication.networkIdOf
import dev.chieppa.ease.runtime.ClientWorld
import dev.chieppa.ease.runtime.InputScript

/**
 * The sample's scripted input, run on a client: every [STEER_EVERY] ticks it sends the host a
 * command steering the client's courier a quarter turn, so the courier traces a square loop moved
 * only by commands. The courier is the one the game gave the client when it joined (see
 * [SampleGame.peerJoined]); until its spawn arrives, the script waits.
 */
public class CourierScript : InputScript {
  private var couriers: Family? = null

  override fun send(client: ClientWorld) {
    val tick = client.tick
    if (tick == 0L || tick % STEER_EVERY != 0L) return
    val courier = courier(client) ?: return
    val turn = (tick / STEER_EVERY % TURNS).toInt()
    client.send(Steer(courier, DIRECTION_X[turn] * SPEED, DIRECTION_Y[turn] * SPEED))
  }

  private fun courier(client: ClientWorld): NetworkId? {
    val world = client.world.world
    val family = couriers ?: world.family { all(Courier) }.also { couriers = it }
    return if (family.isEmpty) null else world.networkIdOf(family.first())
  }

  public companion object {
    /** Ticks between two steers: half a second at the default rate. */
    public const val STEER_EVERY: Long = 30

    /** How fast the courier moves, in world units a second. */
    public const val SPEED: Float = 4f

    private const val TURNS = 4

    /** Right, up, left, down: the courier turns left a quarter at each steer. */
    private val DIRECTION_X = floatArrayOf(1f, 0f, -1f, 0f)
    private val DIRECTION_Y = floatArrayOf(0f, 1f, 0f, -1f)

    /** The courier as it spawns: white, at the center, moving right. */
    public val COURIER: Square =
      Square(x = 0f, y = 0f, speedX = SPEED, speedY = 0f, size = 0.7f, rgb = 0xF2F2F2)
  }
}
