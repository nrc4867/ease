package dev.chieppa.ease.sample

import dev.chieppa.ease.agent.AgentHost
import dev.chieppa.ease.agent.McpTestClient
import dev.chieppa.ease.agent.list
import dev.chieppa.ease.agent.server.ConnectCommand
import dev.chieppa.ease.agent.text
import dev.chieppa.ease.runtime.GameModules
import java.util.concurrent.Executors
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

/**
 * The sample game in the agent host, as an agent reaches it: over MCP on 127.0.0.1, through a
 * session opened with the host's token.
 */
internal class SampleAgent : AutoCloseable {
  private val worldThread = Executors.newSingleThreadExecutor()
  private val host = AgentHost(GameModules.load(), TOKEN, worldThread.asCoroutineDispatcher())
  private val port = runBlocking { host.start(0) }
  val client = McpTestClient(ConnectCommand.url(port), TOKEN).also { it.initialize() }

  /** The result of calling tool [name] with [arguments]. */
  fun call(name: String, arguments: JsonObject = JsonObject(emptyMap())): JsonObject =
    client.callTool(name, arguments)

  /** The JSON of [result]'s first text part. */
  fun json(result: JsonObject): JsonElement {
    val part = content(result).first { it.text("type") == "text" }
    return Json.parseToJsonElement(part.text("text"))
  }

  /** [result]'s content parts. */
  fun content(result: JsonObject): List<JsonObject> = result.list("content").map { it.jsonObject }

  override fun close() {
    runBlocking { host.stop() }
    worldThread.shutdown()
  }

  private companion object {
    const val TOKEN = "sample-token"
  }
}
