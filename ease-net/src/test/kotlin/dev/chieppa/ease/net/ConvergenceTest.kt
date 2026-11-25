package dev.chieppa.ease.net

import dev.chieppa.ease.ecs.replication.PeerId
import dev.chieppa.ease.net.session.ClientSession
import dev.chieppa.ease.net.transport.LoopbackTransport
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * A client's world converges to the host's: through commands the host checks and applies, and
 * through delta snapshots that survive loss and reordering.
 */
class ConvergenceTest {
  @Test
  fun `commands a client sends reach the host and its snapshots bring the client along`() {
    val session = Session(entities = ENTITIES)

    repeat(TICKS) { tick ->
      session.clientSession.sendCommand(session.tick, Push(session.idOf(tick % ENTITIES), STEP))
      session.step()
    }
    repeat(SETTLE) { session.step() }

    assertTrue(session.refused.isEmpty(), session.refused.toString())
    val (host, client) = session.states()
    assertEquals(host, client)
    val (hostX, clientX) = session.xs(0)
    assertEquals(STEP * TICKS / ENTITIES, hostX)
    assertEquals(hostX, clientX)
  }

  @Test
  fun `the client converges when snapshots are lost and reordered`() {
    val random = Random(SEED)
    val fates = List(TICKS) { Fate.entries[random.nextInt(Fate.entries.size)] }
    val session =
      Session(ENTITIES) { end -> ScriptedConnection(end) { fates.getOrElse(it) { Fate.DELIVER } } }

    repeat(TICKS) { tick ->
      session.moveOnHost(random.nextInt(ENTITIES), random.nextInt(VALUES).toFloat())
      if (tick % CHANGE_BACK == 0) session.moveOnHost(0, 0f)
      session.step()
    }
    repeat(SETTLE) { session.step() }

    val (host, client) = session.states()
    assertEquals(host, client)
  }

  @Test
  fun `a value changed and changed back while a snapshot is lost still converges`() {
    val drop = setOf(1)
    val session =
      Session(1) { end -> ScriptedConnection(end) { if (it in drop) Fate.DROP else Fate.DELIVER } }
    session.step()
    session.moveOnHost(0, 1f)
    session.step()
    session.moveOnHost(0, 0f)
    repeat(SETTLE) { session.step() }

    assertEquals(0f to 0f, session.xs(0))
  }

  @Test
  fun `once acknowledged, unchanged entities are left out of snapshots`() {
    var wrapped: ScriptedConnection? = null
    val session =
      Session(ENTITIES) { end -> ScriptedConnection(end) { Fate.DELIVER }.also { wrapped = it } }
    repeat(SETTLE) { session.step() }
    val quiet = checkNotNull(wrapped).lastLength
    session.moveOnHost(0, 2f)
    session.step()
    val oneChange = checkNotNull(wrapped).lastLength
    repeat(SETTLE) { session.step() }
    for (index in 0 until ENTITIES) session.moveOnHost(index, 3f)
    session.step()
    val allChange = checkNotNull(wrapped).lastLength

    assertTrue(quiet < oneChange && oneChange < allChange, "$quiet, $oneChange, $allChange")
    assertEquals(3f to 3f, session.xs(0))
  }

  @Test
  fun `a client that connects later receives every field`() {
    val session = Session(ENTITIES)
    session.moveOnHost(1, 3f)
    repeat(SETTLE) { session.step() }
    val late = PeerId(2)
    val link = LoopbackTransport().connect(late)
    val world = session.lateClient(late)
    session.hostSession.connect(link.hostEnd)
    val client = ClientSession(world.first, session.codec, link.clientEnd, session.settings)

    client.receive()
    session.step()
    client.receive()

    assertEquals(session.states().first, world.second())
  }

  @Test
  fun `snapshots report the last input tick the host processed`() {
    val session = Session(1)
    repeat(SETTLE) { session.step() }

    assertEquals(session.tick - 1, session.clientSession.lastProcessedInputTick)
    assertEquals(session.tick - 1, session.clientSession.latestSnapshotTick)
  }

  private companion object {
    const val ENTITIES = 8
    const val TICKS = 200
    const val SETTLE = 4
    const val STEP = 0.5f
    const val SEED = 4_242L
    const val VALUES = 10
    const val CHANGE_BACK = 7
  }
}
