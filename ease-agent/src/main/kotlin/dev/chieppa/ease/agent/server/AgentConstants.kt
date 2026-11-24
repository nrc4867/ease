package dev.chieppa.ease.agent.server

/** Where the agent server listens and what it calls itself. */
public object AgentConstants {
  /** The only address it listens on. */
  public const val HOST: String = "127.0.0.1"

  /** The other name a local client may use for [HOST]. */
  public const val LOCALHOST: String = "localhost"

  /** The path of its one streamable HTTP endpoint. */
  public const val PATH: String = "/mcp"

  /** The port it listens on unless told another. */
  public const val DEFAULT_PORT: Int = 47_840

  /** Its name, to the client and in the command that connects Claude Code. */
  public const val NAME: String = "ease"

  /** Its version, as the client sees it. */
  public const val VERSION: String = "0.1.0"

  /** The environment variable a fixed token is read from. */
  public const val TOKEN_ENV: String = "EASE_AGENT_TOKEN"

  /** How long a client's session lasts without a request. */
  public const val SESSION_IDLE_MINUTES: Int = 10

  /** How long a stopping server lets requests in flight finish. */
  public const val STOP_GRACE_MILLIS: Long = 200L

  /** How long a stopping server waits in all. */
  public const val STOP_TIMEOUT_MILLIS: Long = 1_000L

  /** What the server tells a client's model about itself when it connects. */
  public const val INSTRUCTIONS: String =
    "Ease is a game engine. This headless host holds one game world, frozen at its current " +
      "tick. Use describe for the component and command types, query to read entities, capture " +
      "for a picture through the game's camera, test to replay the log or run ticks on a " +
      "scratch copy, and logs for what the host recorded."
}
