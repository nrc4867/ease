package dev.chieppa.ease.agent.server

import java.security.SecureRandom
import java.util.Base64

/** Bearer tokens for the agent server. */
public object AgentToken {
  private const val BYTES = 32

  /** A new random token, as URL-safe text. */
  public fun generate(random: SecureRandom = SecureRandom()): String {
    val bytes = ByteArray(BYTES).also(random::nextBytes)
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
  }

  /** [fromEnvironment] when it holds a token, else a new one. */
  public fun resolve(fromEnvironment: String?): String =
    fromEnvironment?.trim()?.takeIf { it.isNotEmpty() } ?: generate()
}
