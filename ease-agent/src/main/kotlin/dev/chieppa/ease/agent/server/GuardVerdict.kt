package dev.chieppa.ease.agent.server

private const val HTTP_OK = 200
private const val HTTP_UNAUTHORIZED = 401
private const val HTTP_FORBIDDEN = 403

/** What [RequestGuard] makes of a request: let through, or refused with an HTTP [status]. */
public enum class GuardVerdict(public val status: Int) {
  Allowed(HTTP_OK),

  /** No bearer token, or not the server's. */
  Unauthorized(HTTP_UNAUTHORIZED),

  /** Sent by a web page: it carries an Origin. */
  BrowserOrigin(HTTP_FORBIDDEN),

  /** Addressed to a host name other than this machine's loopback, as a DNS rebinding page is. */
  WrongHost(HTTP_FORBIDDEN),
}
