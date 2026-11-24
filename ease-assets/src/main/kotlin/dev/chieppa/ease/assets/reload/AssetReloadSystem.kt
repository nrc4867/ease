package dev.chieppa.ease.assets.reload

import com.github.quillraven.fleks.IntervalSystem
import dev.chieppa.ease.assets.loading.AssetStore

/**
 * The world's link to its assets: [ReloadAsset] finds the world's [store] through this system, so a
 * command from the editor, an agent or a peer reloads assets the same way a local file change does.
 * It has no per-tick work.
 */
public class AssetReloadSystem(public val store: AssetStore) : IntervalSystem() {
  override fun onTick(): Unit = Unit
}
