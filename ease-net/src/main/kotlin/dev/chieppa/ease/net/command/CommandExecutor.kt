package dev.chieppa.ease.net.command

import dev.chieppa.ease.commands.Command
import dev.chieppa.ease.ecs.replication.PeerId

/**
 * Applies a command that passed every check, such as through the host's command history so it is
 * logged. Throws `CommandRejected`, changing nothing, when the command cannot apply.
 */
public fun interface CommandExecutor {
  /** Applies [command], which [sender] sent stamped with its [tick]. */
  public fun execute(command: Command, sender: PeerId, tick: Long)
}
