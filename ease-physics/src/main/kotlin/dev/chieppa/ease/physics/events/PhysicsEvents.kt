package dev.chieppa.ease.physics.events

import com.github.quillraven.fleks.Entity

/**
 * The listeners a world's physics reports to, called in the order they were added. Adding or
 * removing a listener makes a new list, so a report already under way reaches exactly the listeners
 * it started with, whatever they add or remove meanwhile. Reporting allocates nothing.
 */
public class PhysicsEvents {
  private var listeners: Array<PhysicsListener> = emptyArray()

  /** Adds [listener], which hears every report from now until it is [remove]d. */
  public fun add(listener: PhysicsListener) {
    listeners += listener
  }

  /** Removes [listener]; nothing happens when it was not added. */
  public fun remove(listener: PhysicsListener) {
    val index = listeners.indexOfFirst { it === listener }
    if (index >= 0) listeners = listeners.filterIndexed { at, _ -> at != index }.toTypedArray()
  }

  internal fun triggerEntered(trigger: Entity, sensor: Entity) {
    val current = listeners
    for (index in current.indices) current[index].triggerEntered(trigger, sensor)
  }

  internal fun triggerExited(trigger: Entity, sensor: Entity) {
    val current = listeners
    for (index in current.indices) current[index].triggerExited(trigger, sensor)
  }

  internal fun footstep(character: Entity, surface: Int) {
    val current = listeners
    for (index in current.indices) current[index].footstep(character, surface)
  }

  internal fun groundSurfaceChanged(character: Entity, surface: Int) {
    val current = listeners
    for (index in current.indices) current[index].groundSurfaceChanged(character, surface)
  }
}
