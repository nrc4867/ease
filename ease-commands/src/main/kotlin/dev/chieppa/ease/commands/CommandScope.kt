package dev.chieppa.ease.commands

/** Which peers may send a command to the world's authority, checked from entity ownership. */
public enum class CommandScope {
  /** Only the authority's own peer: its editor, agents, scripts and game code. */
  LOCAL,

  /** Any peer that owns every entity the command targets; a command with no targets is refused. */
  OWNED_ENTITIES,

  /** Any connected peer, such as for a chat line or a ready flag. */
  ANY_PEER,
}
