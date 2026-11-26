package dev.chieppa.ease.animation.runtime

import dev.chieppa.ease.animation.assets.AnimatorAsset
import dev.chieppa.ease.animation.assets.ClipAsset
import dev.chieppa.ease.animation.assets.SkinAsset
import dev.chieppa.ease.assets.Guid
import dev.chieppa.ease.assets.loading.AssetLoadException
import dev.chieppa.ease.assets.loading.AssetStore
import kotlin.reflect.KClass

/**
 * Where animation finds its assets: animators, clips and skins by GUID, each null when it is
 * missing or fails to load. [changes] moves on whenever any asset may have changed, so a user
 * recompiles what it made from them. Looking up a loaded asset allocates nothing.
 */
public interface AnimationAssets {
  /** Moves on whenever an asset may have changed. */
  public val changes: Int

  /** Why each asset that failed to load did, one line each. */
  public val problems: Collection<String>

  /** The animator asset [guid], or null. */
  public fun animator(guid: Guid): AnimatorAsset?

  /** The clip asset [guid], or null. */
  public fun clip(guid: Guid): ClipAsset?

  /** The skin asset [guid], or null. */
  public fun skin(guid: Guid): SkinAsset?

  public companion object {
    /**
     * The assets of [store], through its handles, so a reloaded asset is read anew. An asset that
     * fails to load is listed in [problems] until the store next reloads something.
     */
    public fun of(store: AssetStore): AnimationAssets = StoreAnimationAssets(store)
  }
}

/** [AnimationAssets] read through an [AssetStore]. */
private class StoreAnimationAssets(private val store: AssetStore) : AnimationAssets {
  private val failed = LinkedHashMap<Guid, String>()
  private var seen = store.changes

  override val changes: Int
    get() = store.changes

  override val problems: Collection<String>
    get() = failed.values

  override fun animator(guid: Guid): AnimatorAsset? = get(guid, ANIMATOR)

  override fun clip(guid: Guid): ClipAsset? = get(guid, CLIP)

  override fun skin(guid: Guid): SkinAsset? = get(guid, SKIN)

  private fun <T : Any> get(guid: Guid, type: KClass<T>): T? {
    if (store.changes != seen) {
      seen = store.changes
      failed.clear()
    }
    if (failed.containsKey(guid)) return null
    return try {
      store.handle(guid, type).value
    } catch (failure: AssetLoadException) {
      failed[guid] = failure.message.orEmpty()
      null
    }
  }

  private companion object {
    /** The asset types, made once: a class literal makes a new object each time it is used. */
    val ANIMATOR: KClass<AnimatorAsset> = AnimatorAsset::class
    val CLIP: KClass<ClipAsset> = ClipAsset::class
    val SKIN: KClass<SkinAsset> = SkinAsset::class
  }
}
