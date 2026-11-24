package dev.chieppa.ease.commands

import com.github.quillraven.fleks.World
import dev.chieppa.ease.ecs.replication.NetworkId

/**
 * A change to a world, and the only way anything outside the simulation changes one: the editor,
 * agents, scripts, plugins and the network all send commands. A command is serializable data,
 * `@Serializable` and registered with a [CommandRegistry], so it can be logged, undone, sent and
 * replayed.
 */
public interface Command {
  /**
   * Who may send this command from another peer: see [CommandScope]. The authority checks it, with
   * [targets], before [validate].
   */
  public val scope: CommandScope
    get() = CommandScope.LOCAL

  /**
   * The entities this command acts on, which a peer sending it under [CommandScope.OWNED_ENTITIES]
   * must own every one of.
   */
  public fun targets(): List<NetworkId> = emptyList()

  /**
   * Why this command may not apply to [world] under the game's rules, such as a range, cooldown or
   * resource, or null when it may. It reads the world and never changes it.
   */
  public fun validate(world: World): String? = null

  /**
   * Applies this command to [world] and returns the command that undoes it exactly. Throws
   * [CommandRejected], leaving the world unchanged, when the command cannot apply.
   */
  public fun apply(world: World): Command
}
