package dev.chieppa.ease.assets.reload

import dev.chieppa.ease.commands.CommandRegistry

/** Registers the asset commands, so a world's codec can log and send them. */
public fun CommandRegistry.registerAssetCommands() {
  command(ReloadAsset::class, ReloadAsset.serializer())
}
