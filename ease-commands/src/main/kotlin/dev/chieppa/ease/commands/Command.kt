package dev.chieppa.ease.commands

import com.github.quillraven.fleks.World

/**
 * A change to a world, and the only way anything outside the simulation changes one: the editor,
 * agents, scripts, plugins and the network all send commands. A command is serializable data,
 * `@Serializable` and registered with a [CommandRegistry], so it can be logged, undone, sent and
 * replayed.
 */
public interface Command {
  /**
   * Applies this command to [world] and returns the command that undoes it exactly. Throws
   * [CommandRejected], leaving the world unchanged, when the command cannot apply.
   */
  public fun apply(world: World): Command
}
