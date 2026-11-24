package dev.chieppa.ease.assets.reload

import dev.chieppa.ease.assets.Guid
import dev.chieppa.ease.assets.database.AssetChange
import dev.chieppa.ease.assets.database.AssetDatabase
import dev.chieppa.ease.assets.loading.AssetStore
import dev.chieppa.ease.assets.loading.GONE_HASH
import dev.chieppa.ease.commands.CommandHistory
import dev.chieppa.ease.commands.CommandOrigin
import dev.chieppa.ease.commands.CommandRejected

/**
 * Carries file changes into a running world. At each tick boundary, [beforeTick] syncs the asset
 * database with the file watcher and sends a [ReloadAsset] command through the world's history for
 * every changed, added, moved or removed asset that something holds a handle on. The command is
 * logged and replicates like any other, so every peer reloads at the same tick.
 *
 * A file can change again between the sync and the reload, as an editor saves in steps. The
 * rejected reload leaves the world as it was and marks the asset's handles with the reason; the
 * asset is tried again at each tick until a reload goes through or a newer change replaces it. A
 * tick never fails because of an asset.
 */
public class AssetSync(
  private val database: AssetDatabase,
  private val store: AssetStore,
  private val history: CommandHistory,
) {
  private val failed = LinkedHashSet<Guid>()

  /** Applies the file changes waiting since the last call, as commands stamped with [tick]. */
  public fun beforeTick(tick: Long) {
    val changes = database.sync()
    for (change in changes) reload(change.guid, commandFor(change), tick)
    if (failed.isNotEmpty()) retry(changes.map { it.guid }.toSet(), tick)
  }

  private fun retry(already: Set<Guid>, tick: Long) {
    for (guid in failed.toList()) {
      if (guid !in already) reload(guid, commandFor(guid), tick)
    }
  }

  @Suppress("SwallowedException") // The store marked the handles with the reason.
  private fun reload(guid: Guid, command: ReloadAsset, tick: Long) {
    if (!store.isLoaded(guid)) {
      failed.remove(guid)
      return
    }
    try {
      history.execute(command, tick, CommandOrigin.GAME)
      failed.remove(guid)
    } catch (_: CommandRejected) {
      failed.add(guid)
    }
  }

  private fun commandFor(change: AssetChange): ReloadAsset =
    when (change) {
      is AssetChange.Removed -> ReloadAsset(change.guid, GONE_HASH)
      is AssetChange.Added -> ReloadAsset(change.guid, change.record.contentHash)
      is AssetChange.Changed -> ReloadAsset(change.guid, change.record.contentHash)
      is AssetChange.Moved -> ReloadAsset(change.guid, change.record.contentHash)
    }

  private fun commandFor(guid: Guid): ReloadAsset =
    ReloadAsset(guid, database[guid]?.contentHash ?: GONE_HASH)
}
