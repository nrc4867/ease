package dev.chieppa.ease.commands

import com.github.quillraven.fleks.World

/**
 * A component that may refuse to go on a new entity, such as an identity that must stay unique in
 * its world. Every command that creates an entity from a snapshot asks each such component in its
 * validation, so a refused entity is never half built.
 */
public interface CreationCheck {
  /** Why an entity carrying this component cannot be created in [world], or null when it can. */
  public fun creationRefusal(world: World): String?
}
