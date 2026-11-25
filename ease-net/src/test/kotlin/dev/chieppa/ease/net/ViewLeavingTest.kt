package dev.chieppa.ease.net

import com.github.quillraven.fleks.Entity
import dev.chieppa.ease.net.relevancy.Area
import dev.chieppa.ease.net.relevancy.AreaFilter
import dev.chieppa.ease.net.relevancy.DistanceFilter
import dev.chieppa.ease.net.relevancy.Team
import dev.chieppa.ease.net.relevancy.TeamFilter
import dev.chieppa.ease.net.view.ViewSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * Only an entity out of range waits out the grace ticks: one a filter rules out, such as a team or
 * area the viewer is not in, leaves the view at once, and with no grace an entity out of range
 * leaves on the tick it goes out.
 */
class ViewLeavingTest {
  private fun session(grace: Int) =
    Session(
      entities = 2,
      owned = 0,
      view = ViewSettings(listOf(DistanceFilter(RADIUS), TeamFilter, AreaFilter), grace),
    )

  @Test
  fun `an entity moved to another team leaves at once`() {
    val session = session(LONG_GRACE)
    session.step()
    assertNotEquals(Entity.NONE, session.mirror(1))

    with(session.host) { session.held[1].configure { it += Team(OTHER) } }
    session.step()

    assertEquals(Entity.NONE, session.mirror(1))
    assertNotEquals(Entity.NONE, session.mirror(0))
  }

  @Test
  fun `an entity moved to another area leaves at once`() {
    val session = session(LONG_GRACE)
    session.step()

    with(session.host) { session.held[1].configure { it += Area(OTHER) } }
    session.step()

    assertEquals(Entity.NONE, session.mirror(1))
  }

  @Test
  fun `with no grace an entity out of range leaves on the tick it goes out`() {
    val session = session(grace = 0)
    session.step()
    assertNotEquals(Entity.NONE, session.mirror(1))

    session.moveOnHost(1, RADIUS * 2)
    session.step()

    assertEquals(Entity.NONE, session.mirror(1))
  }

  private companion object {
    const val RADIUS = 10f
    const val LONG_GRACE = 50
    const val OTHER = 4
  }
}
