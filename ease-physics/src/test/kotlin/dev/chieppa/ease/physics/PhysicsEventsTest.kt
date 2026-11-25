package dev.chieppa.ease.physics

import com.github.quillraven.fleks.Entity
import dev.chieppa.ease.physics.events.PhysicsEvents
import dev.chieppa.ease.physics.events.PhysicsListener
import kotlin.test.Test
import kotlin.test.assertEquals

/** Listeners added or removed while a report is under way leave that report's listeners alone. */
class PhysicsEventsTest {
  private val heard = mutableListOf<String>()
  private val events = PhysicsEvents()

  private inner class Named(val name: String, val onStep: () -> Unit = {}) : PhysicsListener {
    override fun footstep(character: Entity, surface: Int) {
      heard += name
      onStep()
    }
  }

  @Test
  fun `a listener removing itself while hearing a report does not skip the next one`() {
    lateinit var first: Named
    first = Named("first") { events.remove(first) }
    events.add(first)
    events.add(Named("second"))

    events.footstep(Entity(0), 0)
    events.footstep(Entity(0), 0)

    assertEquals(listOf("first", "second", "second"), heard)
  }

  @Test
  fun `a listener added while a report is under way hears the next one`() {
    events.add(Named("first") { events.add(Named("late")) })

    events.footstep(Entity(0), 0)

    assertEquals(listOf("first"), heard)
  }
}
