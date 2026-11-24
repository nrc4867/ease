package dev.chieppa.ease.net.command

import dev.chieppa.ease.commands.Command
import dev.chieppa.ease.ecs.replication.PeerId

/**
 * Applies a command that passed every check, such as through the host's command history so it is
 * logged. Throws `CommandRejected`, changing nothing, when the command cannot apply.
 */
public fun interface CommandExecutor {
  /**
   * Applies [command] from [sender] at the host's [tick]. The tick the sender stamped is never
   * passed on: it is the sender's claim, and the log and history keep only the host's ticks.
   */
  public fun execute(command: Command, sender: PeerId, tick: Long)
}
