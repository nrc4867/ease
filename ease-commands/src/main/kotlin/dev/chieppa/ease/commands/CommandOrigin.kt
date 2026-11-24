package dev.chieppa.ease.commands

/**
 * Who sent a command, kept in the log and the undo history so each change carries its source. Only
 * changes the local user asked for, [undoable], enter the user's undo history.
 */
public enum class CommandOrigin(
  /** Whether a change from this origin can be undone and redone by the local user. */
  public val undoable: Boolean
) {
  /** A person, through the editor or a game's controls. */
  USER(undoable = true),

  /** An agent, through the agent tools, editing on the user's behalf. */
  AGENT(undoable = true),

  /** A script or a tool plugin running on its own, such as scripted input. */
  SCRIPT(undoable = false),

  /** A peer, over the network. */
  NETWORK(undoable = false),

  /** The game's or the engine's own code, such as an asset reload. */
  GAME(undoable = false),
}
