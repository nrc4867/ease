package dev.chieppa.ease.agent

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

/**
 * A bare MCP client over streamable HTTP for tests: posts JSON-RPC to the agent server at [url]
 * with [token], as an MCP client such as Claude Code does.
 */
public class McpTestClient(private val url: String, private val token: String?) {
  private val http = HttpClient.newHttpClient()
  private var session: String? = null
  private var nextId = 1

  /** Posts [body] with the client's headers, plus an [origin] or a [token] other than its own. */
  public fun post(body: String, origin: String? = null): HttpResponse<String> {
    val request =
      HttpRequest.newBuilder(URI.create(url))
        .header("Content-Type", "application/json")
        .header("Accept", "application/json, text/event-stream")
        .apply {
          token?.let { header("Authorization", "Bearer $it") }
          origin?.let { header("Origin", it) }
          session?.let { header("Mcp-Session-Id", it) }
        }
        .POST(HttpRequest.BodyPublishers.ofString(body))
        .build()
    return http.send(request, HttpResponse.BodyHandlers.ofString())
  }

  /** Opens a session; fails unless the server accepts it. */
  public fun initialize() {
    val response = post(INITIALIZE)
    check(response.statusCode() == OK) { "initialize: ${response.statusCode()} ${response.body()}" }
    session = response.headers().firstValue("Mcp-Session-Id").get()
    post("""{"jsonrpc":"2.0","method":"notifications/initialized"}""")
  }

  /** The tools the server lists. */
  public fun listTools(): JsonArray =
    request("tools/list", JsonObject(emptyMap())).getValue("tools").jsonArray

  /** The result of calling tool [name] with [arguments]: its `content` and `isError`. */
  public fun callTool(name: String, arguments: JsonObject = JsonObject(emptyMap())): JsonObject =
    request(
      "tools/call",
      buildJsonObject {
        put("name", name)
        put("arguments", arguments)
      },
    )

  private fun request(method: String, params: JsonObject): JsonObject {
    val body = buildJsonObject {
      put("jsonrpc", "2.0")
      put("id", nextId++)
      put("method", method)
      put("params", params)
    }
    val response = post(body.toString())
    check(response.statusCode() == OK) { "$method: ${response.statusCode()} ${response.body()}" }
    val reply = Json.parseToJsonElement(response.body()).jsonObject
    return reply["result"]?.jsonObject ?: error("$method: ${response.body()}")
  }

  public companion object {
    private const val OK = 200

    /** The initialize request a client opens a session with. */
    public const val INITIALIZE: String =
      """{"jsonrpc":"2.0","id":0,"method":"initialize","params":{"protocolVersion":""" +
        """"2025-06-18","capabilities":{},"clientInfo":{"name":"test","version":"1"}}}"""
  }
}
