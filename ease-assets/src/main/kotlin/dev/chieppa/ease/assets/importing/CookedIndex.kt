package dev.chieppa.ease.assets.importing

import dev.chieppa.ease.assets.Guid
import java.nio.file.Path

/** The cooked assets of a cache by sub-asset GUID, and the sources each import read. */
internal class CookedIndex(
  private val cache: ImportCache,
  private val attempts: ImportAttempts,
) {
  private var subAssets: Map<Guid, SubAsset> = build()

  /** The cooked asset whose sub-asset GUID is [guid], or null when none is stored. */
  fun subAsset(guid: Guid): SubAsset? = subAssets[guid]

  /**
   * The sources whose last good result read the file at [path] besides their own source, or whose
   * latest failed import asked for it.
   */
  fun dependents(path: Path): List<Guid> {
    val key = path.joinToString("/")
    return cache.guids().filter { key in cache.inputs(it) || key in attempts.of(it) }
  }

  /** Indexes the cache again, after results were stored or removed. */
  fun rebuild() {
    subAssets = build()
  }

  private fun build(): Map<Guid, SubAsset> =
    cache
      .guids()
      .flatMap { parent -> cache.names(parent).map { SubAsset(parent, it) } }
      .associateBy { it.guid }
}
