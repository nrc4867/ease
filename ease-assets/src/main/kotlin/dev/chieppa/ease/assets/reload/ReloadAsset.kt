package dev.chieppa.ease.assets.reload

import com.github.quillraven.fleks.World
import dev.chieppa.ease.annotations.Doc
import dev.chieppa.ease.assets.Guid
import dev.chieppa.ease.assets.loading.AssetOutOfDate
import dev.chieppa.ease.commands.Command
import dev.chieppa.ease.commands.CommandRejected
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Doc(
  "Makes the world use the current contents of an asset's file. Reloading is idempotent, so its " +
    "undo reloads again. Rejected when this world's copy of the file differs from the hash."
)
@Serializable
@SerialName("ease.ReloadAsset")
public class ReloadAsset(
  @Doc("The asset whose file changed.") public val asset: Guid,
  @Doc("The file's fingerprint after the change (`sha256:` and hex); empty when the file is gone.")
  public val contentHash: String,
) : Command {
  @Suppress("SwallowedException") // The rejection carries the out-of-date message.
  override fun apply(world: World): Command {
    val system =
      world.systemOrNull<AssetReloadSystem>()
        ?: throw CommandRejected("The world has no asset reload system")
    try {
      system.store.reload(asset, contentHash)
    } catch (stale: AssetOutOfDate) {
      throw CommandRejected(stale.message.orEmpty())
    }
    return ReloadAsset(asset, contentHash)
  }
}
