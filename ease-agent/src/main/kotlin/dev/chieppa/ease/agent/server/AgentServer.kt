package dev.chieppa.ease.agent.server

import dev.chieppa.ease.agent.HostLog
import dev.chieppa.ease.agent.tools.AgentTool
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.request.header
import io.ktor.server.response.respond
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.mcpStreamableHttp
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.minutes

/**
 * The agent's MCP server: the protocol through the MCP SDK's streamable HTTP transport, served by
 * Ktor's CIO engine on 127.0.0.1 only, offering [tools] and nothing else. Every request passes
 * [RequestGuard] first, holding [token]. Tool failures are recorded in [log].
 */
public class AgentServer(private val tools: List<AgentTool>, token: String, log: HostLog) {
  private val listening = AtomicInteger()
  private val guard = RequestGuard(token) { listening.get() }
  private val calls = ToolCalls(log)
  private var engine: EmbeddedServer<*, *>? = null

  /** Starts listening on [port] (0 for any free port) and gives the port; throws when it cannot. */
  public suspend fun start(port: Int): Int {
    val server =
      embeddedServer(CIO, host = AgentConstants.HOST, port = port) { configure(protocol()) }
    server.startSuspend(wait = false)
    engine = server
    val connectors = server.engine.resolvedConnectors()
    val bound = connectors.first().port
    listening.set(bound)
    return bound
  }

  /** Stops listening; requests in flight get a moment to finish. */
  public fun stop() {
    engine?.stop(AgentConstants.STOP_GRACE_MILLIS, AgentConstants.STOP_TIMEOUT_MILLIS)
    engine = null
  }

  private fun Application.configure(server: Server) {
    intercept(ApplicationCallPipeline.Plugins) {
      val request = call.request
      val verdict =
        guard.check(
          request.header(HttpHeaders.Host),
          request.header(HttpHeaders.Origin),
          request.header(HttpHeaders.Authorization),
        )
      if (verdict != GuardVerdict.Allowed) {
        call.respond(HttpStatusCode.fromValue(verdict.status))
        finish()
      }
    }
    mcpStreamableHttp(
      path = AgentConstants.PATH,
      allowedHosts = listOf(AgentConstants.HOST, AgentConstants.LOCALHOST),
      sessionIdleTimeout = AgentConstants.SESSION_IDLE_MINUTES.minutes,
    ) {
      server
    }
  }

  /** One protocol server for every session, so each session's tools are the same. */
  private fun protocol(): Server {
    val server =
      Server(
        Implementation(AgentConstants.NAME, AgentConstants.VERSION),
        ServerOptions(ServerCapabilities(tools = ServerCapabilities.Tools(listChanged = false))),
        AgentConstants.INSTRUCTIONS,
      )
    tools.forEach { tool ->
      server.addTool(calls.describe(tool)) { request -> calls.call(tool, request.arguments) }
    }
    return server
  }
}
