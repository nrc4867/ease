package dev.chieppa.ease.net

import dev.chieppa.ease.net.command.CommandLayer
import dev.chieppa.ease.net.command.RateLimit
import dev.chieppa.ease.net.command.RateLimits
import dev.chieppa.ease.net.session.CloseReason
import dev.chieppa.ease.net.session.NetSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Commands over a rate limit, per connection or per command type, are refused and change nothing; a
 * client that keeps flooding is disconnected. Limits count ticks, never time.
 */
class RateLimitTest {
  private fun session(limits: RateLimits) =
    Session(entities = 1, settings = NetSettings(rateLimits = limits))

  private fun flood(session: Session, count: Int) {
    repeat(count) { session.clientSession.sendCommand(session.tick, Push(session.idOf(0), STEP)) }
  }

  @Test
  fun `a flood in one tick applies only the burst the connection allows`() {
    val limits =
      RateLimits(perConnection = RateLimit(commands = 2, burst = BURST), strikesToClose = FLOOD)
    val session = session(limits)
    flood(session, FLOOD)
    session.step()
    session.step()

    assertEquals(BURST * STEP, session.xs(0).first)
    assertEquals(BURST * STEP, session.xs(0).second)
    val rate = session.refused.count { it.first == CommandLayer.RATE }
    assertEquals(FLOOD - BURST, rate)
  }

  @Test
  fun `a command type takes its own limit over the default`() {
    val limits =
      RateLimits(
        perType = RateLimit(commands = FLOOD),
        byType = mapOf("test.Push" to RateLimit(commands = 1, burst = 1)),
      )
    val session = session(limits)
    flood(session, 3)
    session.step()

    assertEquals(STEP, session.xs(0).first)
    assertEquals(2, session.refused.count { it.first == CommandLayer.RATE })
  }

  @Test
  fun `the allowance refills with ticks`() {
    val session = session(RateLimits(perConnection = RateLimit(commands = 1, ticks = 2, burst = 1)))
    repeat(TICKS) {
      flood(session, 1)
      session.step()
    }

    assertEquals(TICKS / 2 * STEP, session.xs(0).first)
  }

  @Test
  fun `a client that keeps flooding is disconnected`() {
    val limits = RateLimits(perConnection = RateLimit(commands = 1, burst = 1), strikesToClose = 50)
    val session = session(limits)
    repeat(TICKS) {
      flood(session, FLOOD)
      session.step()
    }

    assertTrue(!session.peer.isOpen)
    assertEquals(CloseReason.RATE_LIMITED, session.peer.closeReason)
  }

  private companion object {
    const val STEP = 0.25f
    const val BURST = 5
    const val FLOOD = 1_000
    const val TICKS = 10
  }
}
