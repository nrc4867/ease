package dev.chieppa.ease.assets

import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest

private const val ALGORITHM_PREFIX = "sha256:"
private const val HEX_DIGITS = "0123456789abcdef"
private const val NIBBLE_BITS = 4
private const val NIBBLE_MASK = 0xf
private const val BYTE_MASK = 0xff

/** The fingerprint of a file's bytes: `sha256:` and the SHA-256 as lowercase hex. */
public object ContentHash {
  /** The hash of [bytes]. */
  public fun of(bytes: ByteArray): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
    val out = StringBuilder(ALGORITHM_PREFIX).also { it.ensureCapacity(digest.size * 2) }
    for (byte in digest) {
      val value = byte.toInt() and BYTE_MASK
      out.append(HEX_DIGITS[value shr NIBBLE_BITS]).append(HEX_DIGITS[value and NIBBLE_MASK])
    }
    return out.toString()
  }

  /** The hash of the file at [path]. */
  public fun of(path: Path): String = of(Files.readAllBytes(path))
}
