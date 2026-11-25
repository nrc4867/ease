package dev.chieppa.ease.physics.events

import com.github.quillraven.fleks.Entity

/**
 * Receives what a world's physics reports, during the fixed step that found it, through
 * [PhysicsEvents]. The script system subscribes here; games may too. Each method does nothing
 * unless overridden. Entities are the world's own, valid for the call.
 */
public interface PhysicsListener {
  /** [sensor], a `VolumeSensor`, entered [trigger], a `TriggerVolume` it may report. */
  public fun triggerEntered(trigger: Entity, sensor: Entity) {}

  /** [sensor] left [trigger], unless [trigger] fires once. */
  public fun triggerExited(trigger: Entity, sensor: Entity) {}

  /**
   * [character] took a step on the ground, one stride after the last, on the surface type at index
   * [surface].
   */
  public fun footstep(character: Entity, surface: Int) {}

  /**
   * [character] stands on a different surface type: the one at index [surface], or -1 as it leaves
   * the ground.
   */
  public fun groundSurfaceChanged(character: Entity, surface: Int) {}
}
