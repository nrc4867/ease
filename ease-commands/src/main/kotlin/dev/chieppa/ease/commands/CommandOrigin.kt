package dev.chieppa.ease.commands

/** Who sent a command, kept in the log and the undo history so each change carries its source. */
public enum class CommandOrigin {
  /** A person, through the editor or a game's controls. */
  USER,

  /** An agent, through the agent tools. */
  AGENT,

  /** A script or a tool plugin. */
  SCRIPT,

  /** A peer, over the network. */
  NETWORK,

  /** The game's own code. */
  GAME,
}
