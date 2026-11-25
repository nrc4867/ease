package dev.chieppa.ease.net.session

/** Why a session closed a connection. */
public enum class CloseReason {
  /** The other end, or its transport, closed it. */
  CLOSED,

  /**
   * The host named a scene or node the client's files do not have, so it cannot build what the host
   * spawned: the peers' content differs.
   */
  CONTENT_MISMATCH,

  /** A command message was not a registered command in the binary form. */
  MALFORMED_COMMAND,

  /** An input message did not decode. */
  MALFORMED_INPUT,

  /** A spawn or despawn message did not decode or could not apply. */
  MALFORMED_SPAWN,

  /** A snapshot message did not decode or could not apply. */
  MALFORMED_SNAPSHOT,

  /** The peer kept sending commands over its rate limits. */
  RATE_LIMITED,

  /**
   * The peers' codecs number different command, component or replicated types on the wire, so they
   * would misread each other; they must register the same types.
   */
  TYPES_MISMATCH,
}
