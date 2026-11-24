package dev.chieppa.ease.agent.server

/** The command that adds the agent server to Claude Code, with its address and token. */
public object ConnectCommand {
  /** The command for a server on [port] that takes [token]. */
  public fun of(port: Int, token: String): String =
    "claude mcp add --transport http ${AgentConstants.NAME} ${url(port)} " +
      "--header \"Authorization: Bearer $token\""

  /** The server's endpoint on [port]. */
  public fun url(port: Int): String = "http://${AgentConstants.HOST}:$port${AgentConstants.PATH}"
}
