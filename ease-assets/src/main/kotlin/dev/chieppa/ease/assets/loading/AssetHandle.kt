package dev.chieppa.ease.assets.loading

import dev.chieppa.ease.assets.Guid

/**
 * A loaded asset that follows its file. [value] is always the last version that loaded
 * successfully; a reload that fails leaves it in place and sets [error], so one broken file never
 * takes a running game down.
 */
public class AssetHandle<T : Any> internal constructor(public val guid: Guid, first: T) {
  /** The last good version. */
  public var value: T = first
    private set

  /** Counts successful loads, starting at 1; code that caches a derived value compares it. */
  public var version: Int = 1
    private set

  /** Why the latest reload failed, or null when the latest load worked. */
  public var error: String? = null
    private set

  internal fun replace(next: T) {
    value = next
    version++
    error = null
  }

  internal fun fail(reason: String) {
    error = reason
  }
}
