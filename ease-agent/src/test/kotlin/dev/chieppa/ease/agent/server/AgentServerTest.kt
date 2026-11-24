package dev.chieppa.ease.agent.server

import dev.chieppa.ease.agent.AgentHost
import dev.chieppa.ease.agent.DriftGame
import dev.chieppa.ease.agent.McpTestClient
import dev.chieppa.ease.agent.list
import dev.chieppa.ease.agent.names
import dev.chieppa.ease.agent.text
import java.net.Socket
import java.util.concurrent.Executors
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/** The agent host over real HTTP on 127.0.0.1 and a port the system picks. */
class AgentServerTest {
  private val worldThread = Executors.newSingleThreadExecutor()
  private val host = AgentHost(DriftGame(), TOKEN, worldThread.asCoroutineDispatcher())
  private val port = runBlocking { host.start(0) }
  private val url = ConnectCommand.url(port)

  @AfterTest
  fun stop() {
    runBlocking { host.stop() }
    worldThread.shutdown()
  }

  @Test
  fun `a client with the token lists the five tools and calls one`() {
    val client = McpTestClient(url, TOKEN)
    client.initialize()

    val names = client.listTools().names()
    val result = client.callTool("query", buildJsonObject { put("component", "test.Glow") })

    assertEquals(listOf("describe", "query", "capture", "logs", "test"), names)
    val text = result.list("content")[0].text("text")
    assertTrue("test.Glow" in text, text)
  }

  @Test
  fun `a refused call answers an error the model reads`() {
    val client = McpTestClient(url, TOKEN)
    client.initialize()

    val result = client.callTool("test", buildJsonObject { put("check", "none") })

    assertEquals("true", result.getValue("isError").jsonPrimitive.content)
    assertTrue("Unknown check none" in result.toString(), result.toString())
  }

  @Test
  fun `no token or a wrong one is unauthorized`() {
    assertEquals(401, McpTestClient(url, null).post(McpTestClient.INITIALIZE).statusCode())
    assertEquals(401, McpTestClient(url, "guess").post(McpTestClient.INITIALIZE).statusCode())
  }

  @Test
  fun `a request from a web page is refused`() {
    val client = McpTestClient(url, TOKEN)

    val response = client.post(McpTestClient.INITIALIZE, origin = "http://localhost:$port")

    assertEquals(403, response.statusCode())
  }

  @Test
  fun `a request naming another host is refused`() {
    Socket(AgentConstants.HOST, port).use { socket ->
      val body = McpTestClient.INITIALIZE
      val request =
        "POST /mcp HTTP/1.1\r\nHost: rebound.example:$port\r\nAuthorization: Bearer $TOKEN\r\n" +
          "Content-Type: application/json\r\nContent-Length: ${body.length}\r\n" +
          "Connection: close\r\n\r\n$body"
      socket.getOutputStream().write(request.toByteArray())
      val status = socket.getInputStream().bufferedReader().readLine()
      assertTrue(" 403 " in status, status)
    }
  }

  private companion object {
    const val TOKEN = "test-token"
  }
}
