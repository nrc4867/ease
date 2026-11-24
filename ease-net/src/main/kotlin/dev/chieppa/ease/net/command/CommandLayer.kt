package dev.chieppa.ease.net.command

/** Where the authority refused a command from a peer, in the order the checks run. */
public enum class CommandLayer {
  /** Over a rate limit, for the connection or the command's type. */
  RATE,

  /** Not a command at all: malformed bytes, an unknown type, too long. The connection closes. */
  SHAPE,

  /** The peer may not send this command for these entities: its scope and their owners. */
  PERMISSION,

  /** The command's own `validate`, or its `apply`, refused it under the game's rules. */
  RULES,
}
