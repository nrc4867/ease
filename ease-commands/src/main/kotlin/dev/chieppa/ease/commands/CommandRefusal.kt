package dev.chieppa.ease.commands

/** Why received command bytes were refused. */
public enum class CommandRefusal {
  /** Longer than any command a peer may send. */
  TOO_LONG,

  /** Not a registered command in the binary form: unknown type, wrong shape or cut short. */
  MALFORMED,
}
