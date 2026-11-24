package dev.chieppa.ease.agent

import dev.chieppa.ease.agent.server.AgentConstants
import dev.chieppa.ease.agent.server.AgentToken
import dev.chieppa.ease.agent.server.ConnectCommand
import dev.chieppa.ease.runtime.GameModules
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking

/**
 * Runs the agent host for the one game on the classpath until the process is stopped: `--port`
 * picks the port, `EASE_AGENT_TOKEN` a fixed token. A generated token is printed once, with the
 * command that connects Claude Code.
 */
public fun main(args: Array<String>) {
  val options = AgentOptions.parse(args)
  val fixed = System.getenv(AgentConstants.TOKEN_ENV)
  val token = AgentToken.resolve(fixed)
  val worldThread = Executors.newSingleThreadExecutor { Thread(it, WORLD_THREAD) }
  val host =
    AgentHost(
      GameModules.load(),
      token,
      worldThread.asCoroutineDispatcher(),
      HostLog { println(it.text) },
    )
  val port = runBlocking { host.start(options.port) }
  val shown = if (fixed.isNullOrBlank()) token else "\$${AgentConstants.TOKEN_ENV}"
  println("Connect Claude Code with:")
  println(ConnectCommand.of(port, shown))
  val stopped = CountDownLatch(1)
  Runtime.getRuntime()
    .addShutdownHook(
      Thread {
        runBlocking { host.stop() }
        worldThread.shutdown()
        stopped.countDown()
      }
    )
  stopped.await()
}

private const val WORLD_THREAD = "ease-agent-world"
