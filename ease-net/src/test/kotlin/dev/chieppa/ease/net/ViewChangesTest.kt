package dev.chieppa.ease.net

import com.github.quillraven.fleks.Entity
import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.ecs.replication.Authority
import dev.chieppa.ease.ecs.replication.PeerId
import dev.chieppa.ease.ecs.replication.replication
import dev.chieppa.ease.net.channel.ChannelKind
import dev.chieppa.ease.net.relevancy.DistanceFilter
import dev.chieppa.ease.net.view.ViewSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Entities spawn on a client as it moves near them and despawn as it moves away, once the grace
 * ticks have passed; one moving back and forth across the edge stays. The client's walker is the
 * entity it owns, which the viewer follows; the others stand along x, [SPACING] apart.
 */
class ViewChangesTest {
  private var recorder: RecordingConnection? = null
  private val session =
    Session(
      entities = MARKERS + 1,
      owned = 1,
      view = ViewSettings(listOf(DistanceFilter(RADIUS)), graceTicks = GRACE),
    ) { end ->
      RecordingConnection(end).also { recorder = it }
    }

  init {
    for (index in 1..MARKERS) session.moveOnHost(index, index * SPACING)
    session.peer.viewer.focus(session.idOf(0))
  }

  private fun walkTo(x: Float) {
    session.moveOnHost(0, x)
  }

  /** The indexes of the markers the client holds a copy of. */
  private fun held(): List<Int> = (1..MARKERS).filter { session.mirror(it) != Entity.NONE }

  @Test
  fun `entities spawn and despawn as the client walks along them`() {
    session.step()
    assertEquals(listOf(1), held())

    walkTo(SPACING * 3)
    session.step()
    session.step()
    assertEquals(listOf(1, 2, 3, 4), held())

    repeat(GRACE + 1) { session.step() }
    assertEquals(listOf(2, 3, 4), held())

    walkTo(SPACING * MARKERS)
    repeat(GRACE + 2) { session.step() }
    assertEquals(listOf(MARKERS - 1, MARKERS), held())
    assertTrue(session.clientSession.isOpen)
  }

  @Test
  fun `an entity stays through the grace ticks before it despawns`() {
    session.step()
    walkTo(-SPACING * 2)
    session.step()

    repeat(GRACE - 1) {
      session.step()
      assertEquals(listOf(1), held())
    }
    session.step()
    session.step()
    assertEquals(emptyList(), held())
  }

  @Test
  fun `an entity moving back and forth across the edge never respawns`() {
    session.step()
    val messages = checkNotNull(recorder).count(ChannelKind.RELIABLE_ORDERED)

    repeat(GRACE * 4) { tick ->
      val x = if (tick % 2 == 0) RADIUS + 1f else RADIUS - 1f
      session.host.replication.write(session.held[1], LocalTransform) { this.x = x }
      session.step()
    }

    assertEquals(messages, checkNotNull(recorder).count(ChannelKind.RELIABLE_ORDERED))
    assertEquals(listOf(1), held())
  }

  @Test
  fun `a client always holds the entities it owns, wherever they are`() {
    with(session.host) { session.held[MARKERS][Authority].owner = Session.CLIENT }
    session.step()

    assertEquals(listOf(1, MARKERS), held())
    assertEquals(PeerId.HOST, with(session.client) { session.mirror(MARKERS)[Authority].holder })
  }

  private companion object {
    const val MARKERS = 8
    const val SPACING = 10f
    const val RADIUS = 15f
    const val GRACE = 5
  }
}
