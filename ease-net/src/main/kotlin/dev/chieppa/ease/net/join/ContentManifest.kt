package dev.chieppa.ease.net.join

import dev.chieppa.ease.assets.database.AssetRecord
import java.nio.ByteBuffer
import java.security.MessageDigest

/**
 * The content a peer builds its worlds from, as the join handshake compares it: a SHA-256 [digest]
 * over the GUID and content hash of every shipped asset, then the id and hash of every enabled mod
 * that changes the simulation. Peers whose digests differ would build different worlds from the
 * same spawns, so the host refuses them at join. Each list is sorted first, so the order a peer
 * finds its content in never matters, and every field is hashed behind its length.
 */
public class ContentManifest private constructor(digest: ByteArray) {
  private val bytes = digest.copyOf()

  /** The digest's [DIGEST_BYTES] bytes, a copy. */
  public val digest: ByteArray
    get() = bytes.copyOf()

  /**
   * Whether the [DIGEST_BYTES] bytes of [other] from [offset] are this digest; allocates nothing.
   */
  public fun matches(other: ByteArray, offset: Int): Boolean {
    var same = offset >= 0 && offset + DIGEST_BYTES <= other.size
    var index = 0
    while (same && index < DIGEST_BYTES) {
      same = bytes[index] == other[offset + index]
      index++
    }
    return same
  }

  public companion object {
    /** The digest's length in bytes. */
    public const val DIGEST_BYTES: Int = 32

    /** The content of a peer with no assets and no mods. */
    public val EMPTY: ContentManifest = of()

    /**
     * The manifest of [assets] and [mods]. Mods marked client-only (UI, cosmetics) are left out by
     * the caller, since they never change what a world holds.
     */
    public fun of(
      assets: Collection<AssetRecord> = emptyList(),
      mods: Collection<ContentPart> = emptyList(),
    ): ContentManifest {
      val sha = MessageDigest.getInstance("SHA-256")
      section(sha, ASSETS, assets.map { ContentPart(it.guid.toString(), it.contentHash) })
      section(sha, MODS, mods)
      return ContentManifest(sha.digest())
    }

    private fun section(sha: MessageDigest, name: String, parts: Collection<ContentPart>) {
      field(sha, name)
      sha.update(ByteBuffer.allocate(Int.SIZE_BYTES).putInt(parts.size).array())
      parts.sortedWith(compareBy({ it.id }, { it.hash })).forEach {
        field(sha, it.id)
        field(sha, it.hash)
      }
    }

    /** Hashes [text] behind its length in bytes, so no two lists of fields hash alike. */
    private fun field(sha: MessageDigest, text: String) {
      val bytes = text.toByteArray(Charsets.UTF_8)
      sha.update(ByteBuffer.allocate(Int.SIZE_BYTES).putInt(bytes.size).array())
      sha.update(bytes)
    }

    private const val ASSETS = "assets"
    private const val MODS = "mods"
  }
}
