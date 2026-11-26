package dev.chieppa.ease.animation.events

import com.github.quillraven.fleks.Entity

/**
 * The listeners a world's animators report to, called in the order they were added. Adding or
 * removing a listener makes a new list, so a report already under way reaches exactly the listeners
 * it started with. Reporting allocates nothing.
 */
public class AnimationEvents {
  private var listeners: Array<AnimationListener> = emptyArray()

  /** Adds [listener], which hears every report from now until it is [remove]d. */
  public fun add(listener: AnimationListener) {
    listeners += listener
  }

  /** Removes [listener]; nothing happens when it was not added. */
  public fun remove(listener: AnimationListener) {
    val index = listeners.indexOfFirst { it === listener }
    if (index >= 0) listeners = listeners.filterIndexed { at, _ -> at != index }.toTypedArray()
  }

  internal fun clipEvent(animator: Entity, event: String) {
    val current = listeners
    for (index in current.indices) current[index].clipEvent(animator, event)
  }

  @Suppress("LongParameterList") // The point as three floats, so reporting allocates nothing.
  internal fun probe(animator: Entity, probe: String, surface: Int, x: Float, y: Float, z: Float) {
    val current = listeners
    for (index in current.indices) current[index].probe(animator, probe, surface, x, y, z)
  }
}
