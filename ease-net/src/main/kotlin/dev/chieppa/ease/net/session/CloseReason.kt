package dev.chieppa.ease.net.session

/** Why a session closed a connection. */
public enum class CloseReason {
  /** The other end, or its transport, closed it. */
  CLOSED,

  /** A command message was not a registered command in the binary form. */
  MALFORMED_COMMAND,

  /** An input message did not decode. */
  MALFORMED_INPUT,

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
