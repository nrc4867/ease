package dev.chieppa.ease.physics.events

import com.github.quillraven.fleks.Entity

/**
 * The listeners a world's physics reports to, called in the order they were added. Reporting
 * allocates nothing.
 */
public class PhysicsEvents {
  private var listeners = Array<PhysicsListener>(INITIAL_LISTENERS) { Silent }
  private var count = 0

  /** Adds [listener], which hears every report from now until it is [remove]d. */
  public fun add(listener: PhysicsListener) {
    if (count == listeners.size) listeners = Array(count * 2) { listeners.getOrElse(it) { Silent } }
    listeners[count++] = listener
  }

  /** Removes [listener]; nothing happens when it was not added. */
  public fun remove(listener: PhysicsListener) {
    val index = (0 until count).firstOrNull { listeners[it] === listener } ?: return
    System.arraycopy(listeners, index + 1, listeners, index, count - index - 1)
    listeners[--count] = Silent
  }

  internal fun triggerEntered(trigger: Entity, sensor: Entity) {
    for (index in 0 until count) listeners[index].triggerEntered(trigger, sensor)
  }

  internal fun triggerExited(trigger: Entity, sensor: Entity) {
    for (index in 0 until count) listeners[index].triggerExited(trigger, sensor)
  }

  internal fun footstep(character: Entity, surface: Int) {
    for (index in 0 until count) listeners[index].footstep(character, surface)
  }

  internal fun groundSurfaceChanged(character: Entity, surface: Int) {
    for (index in 0 until count) listeners[index].groundSurfaceChanged(character, surface)
  }

  /** Fills the unused places of the listener array. */
  private object Silent : PhysicsListener

  private companion object {
    const val INITIAL_LISTENERS = 4
  }
}
