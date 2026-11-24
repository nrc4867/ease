package dev.chieppa.ease.agent

import dev.chieppa.ease.agent.server.AgentConstants

/** What the agent host was asked to do on its command line: the [port] to listen on. */
internal data class AgentOptions(val port: Int) {
  companion object {
    fun parse(args: Array<String>): AgentOptions {
      var port = AgentConstants.DEFAULT_PORT
      val rest = args.iterator()
      while (rest.hasNext()) {
        when (val arg = rest.next()) {
          "--port" -> port = rest.next().toInt()
          else -> error("Unknown option $arg; the agent host takes --port <port>")
        }
      }
      return AgentOptions(port)
    }
  }
}
