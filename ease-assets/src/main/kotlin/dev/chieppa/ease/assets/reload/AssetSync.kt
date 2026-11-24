package dev.chieppa.ease.assets.reload

import dev.chieppa.ease.assets.database.AssetChange
import dev.chieppa.ease.assets.database.AssetDatabase
import dev.chieppa.ease.assets.loading.AssetStore
import dev.chieppa.ease.assets.loading.GONE_HASH
import dev.chieppa.ease.commands.CommandHistory
import dev.chieppa.ease.commands.CommandOrigin

/**
 * Carries file changes into a running world. At each tick boundary, [beforeTick] syncs the asset
 * database with the file watcher and sends a [ReloadAsset] command through the world's history for
 * every changed asset that something holds a handle on. The command is logged and replicates like
 * any other, so every peer reloads at the same tick.
 */
public class AssetSync(
  private val database: AssetDatabase,
  private val store: AssetStore,
  private val history: CommandHistory,
) {
  /** Applies the file changes waiting since the last call, as commands stamped with [tick]. */
  public fun beforeTick(tick: Long) {
    for (change in database.sync()) {
      val command =
        when (change) {
          is AssetChange.Changed -> ReloadAsset(change.guid, change.record.contentHash)
          is AssetChange.Removed -> ReloadAsset(change.guid, GONE_HASH)
          is AssetChange.Added,
          is AssetChange.Moved -> null
        }
      if (command != null && store.isLoaded(change.guid)) {
        history.execute(command, tick, CommandOrigin.GAME)
      }
    }
  }
}
