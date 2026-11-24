package dev.chieppa.ease.sample

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.Family
import com.github.quillraven.fleks.World.Companion.family
import dev.chieppa.ease.commands.CommandOrigin
import dev.chieppa.ease.commands.builtin.Spawn
import dev.chieppa.ease.runtime.HostedWorld
import dev.chieppa.ease.runtime.InputScript

/**
 * The sample's scripted input: on the first tick it spawns a white courier square at the center,
 * then every [STEER_EVERY] ticks it steers the courier a quarter turn, so the courier traces a
 * square loop moved only by commands.
 */
public class CourierScript : InputScript {
  private var couriers: Family? = null

  override fun send(world: HostedWorld) {
    val tick = world.tick
    if (tick == 0L) {
      world.execute(Spawn(COURIER.components() + Courier()), CommandOrigin.SCRIPT)
      return
    }
    if (tick % STEER_EVERY != 0L) return
    val turn = (tick / STEER_EVERY % TURNS).toInt()
    world.execute(
      Steer(courier(world), DIRECTION_X[turn] * SPEED, DIRECTION_Y[turn] * SPEED),
      CommandOrigin.SCRIPT,
    )
  }

  private fun courier(world: HostedWorld): Entity {
    val family = couriers ?: world.world.family { all(Courier) }.also { couriers = it }
    return family.first()
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
