package dev.chieppa.ease.agent.server

import java.security.MessageDigest

/**
 * Lets a request through only from a local client that holds the server's [token]: its Host names
 * this machine's loopback on the port the server listens on, which [port] gives; it carries no
 * Origin (web pages do, MCP clients do not); and its Authorization is `Bearer` and the token,
 * compared in constant time.
 */
public class RequestGuard(private val token: String, private val port: () -> Int) {
  /** The verdict on a request with these headers. */
  public fun check(host: String?, origin: String?, authorization: String?): GuardVerdict {
    val local = setOf("${AgentConstants.HOST}:${port()}", "${AgentConstants.LOCALHOST}:${port()}")
    return when {
      host?.lowercase() !in local -> GuardVerdict.WrongHost
      origin != null -> GuardVerdict.BrowserOrigin
      !holdsToken(authorization) -> GuardVerdict.Unauthorized
      else -> GuardVerdict.Allowed
    }
  }

  private fun holdsToken(authorization: String?): Boolean {
    val given =
      authorization
        ?.takeIf { it.startsWith(BEARER, ignoreCase = true) }
        ?.substring(BEARER.length)
        ?.trim() ?: return false
    return MessageDigest.isEqual(
      given.toByteArray(Charsets.UTF_8),
      token.toByteArray(Charsets.UTF_8),
    )
  }

  private companion object {
    const val BEARER = "Bearer "
  }
}
