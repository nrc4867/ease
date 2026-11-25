package dev.chieppa.ease.net.session

import dev.chieppa.ease.net.channel.Connection

/**
 * Why a session closed a connection. The end that closes sends its reason's [code] with the close
 * (see [Connection.close]), so the other end reads the same reason.
 */
public enum class CloseReason {
  /** The other end, or its transport, closed it with no reason a session gives. */
  CLOSED,

  /**
   * The peers' content differs: the join hello carried another content digest, or the host named a
   * scene or node the client's files do not have.
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

  /** The first message was not a join hello. */
  MALFORMED_HELLO,

  /** The peers speak different versions of the session protocol. */
  PROTOCOL_MISMATCH;

  /**
   * The close code that carries this reason to the other end: its place in this list, so a new
   * reason goes at the end and none moves once shipped.
   */
  public val code: Int
    get() = ordinal

  public companion object {
    /** The reason [code] carries; [CLOSED] for a code no reason has. */
    public fun of(code: Int): CloseReason = entries.getOrNull(code) ?: CLOSED
  }
}
