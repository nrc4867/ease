package dev.chieppa.ease.agent

import dev.chieppa.ease.agent.server.AgentConstants
import java.nio.file.Path

/**
 * What the agent host was asked to do on its command line: the [port] to listen on, and the game's
 * [content] folder, when it has one, which the world loads its assets and start scene from.
 */
internal data class AgentOptions(val port: Int, val content: Path? = null) {
  companion object {
    private const val USAGE = "the agent host takes --port <port> and --content <folder>"

    fun parse(args: Array<String>): AgentOptions {
      var options = AgentOptions(AgentConstants.DEFAULT_PORT)
      val rest = args.iterator()
      while (rest.hasNext()) {
        val arg = rest.next()
        check(arg == "--port" || arg == "--content") { "Unknown option $arg; $USAGE" }
        check(rest.hasNext()) { "$arg needs a value; $USAGE" }
        val value = rest.next()
        options =
          if (arg == "--port") options.copy(port = value.toInt())
          else options.copy(content = Path.of(value))
      }
      return options
    }
  }
}
