package dev.chieppa.ease.scene.runtime

import dev.chieppa.ease.assets.Guid
import dev.chieppa.ease.assets.loading.AssetHandle
import dev.chieppa.ease.assets.loading.AssetLoadException
import dev.chieppa.ease.assets.loading.AssetStore
import dev.chieppa.ease.scene.document.SceneDocument
import dev.chieppa.ease.scene.resolve.SceneResolver
import dev.chieppa.ease.scene.resolve.SceneSource
import dev.chieppa.ease.scene.types.SceneTypes

/**
 * Finds the scenes and prefabs a resolve asks for in [store], and keeps what it used: the handle of
 * every file it loaded and, for every file it could not load, the content hash that file had then
 * (null when it did not exist). A [PlacedScene] tracks both to know when to resolve again.
 */
internal class TrackingSource(private val store: AssetStore) : SceneSource {
  /** The handles of the files the resolve loaded. */
  val handles: MutableList<AssetHandle<SceneDocument>> = mutableListOf()

  /** The files the resolve needed but could not load, with the content hash each had. */
  val failed: MutableMap<Guid, String?> = LinkedHashMap()

  override fun document(asset: Guid): SceneDocument? {
    val handle = handleOf(asset)
    if (handle == null) failed[asset] = store.database[asset]?.contentHash else handles += handle
    return handle?.value
  }

  /** A resolver for [types] that reads files through this source. */
  fun resolver(types: SceneTypes): SceneResolver = SceneResolver(types, this) { store.has(it) }

  /** Has [placed] track the files this source used. */
  fun trackIn(placed: PlacedScene) {
    placed.track(handles, failed)
  }

  @Suppress("SwallowedException") // A missing or broken file is reported as a scene problem.
  private fun handleOf(guid: Guid): AssetHandle<SceneDocument>? =
    try {
      store.handle(guid, SceneDocument::class)
    } catch (missing: AssetLoadException) {
      null
    }
}
