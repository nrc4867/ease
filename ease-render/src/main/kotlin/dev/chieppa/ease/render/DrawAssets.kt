package dev.chieppa.ease.render

import dev.chieppa.ease.assets.Guid
import dev.chieppa.ease.assets.loading.AssetLoadException
import dev.chieppa.ease.assets.loading.AssetStore
import dev.chieppa.ease.render.assets.MaterialAsset
import dev.chieppa.ease.render.assets.MeshAsset
import kotlin.reflect.KClass

/**
 * The mesh and material assets an extract draws, through [store]'s handles, so a reloaded asset
 * draws its new version. An asset that fails its first load is drawn as nothing (a mesh) or the
 * fallback (a material) and listed in [problems] until the store next reloads something, when every
 * failed asset is tried again; looking up a loaded asset allocates nothing.
 */
internal class DrawAssets(private val store: AssetStore?) {
  private val failed = LinkedHashMap<Guid, String>()
  private var seenChanges = store?.changes ?: 0

  /** Why each asset that failed its first load did, one line each. */
  val problems: Collection<String>
    get() = failed.values

  /** The mesh asset [guid], or null when it is missing or fails to load. */
  fun mesh(guid: Guid): MeshAsset? = get(guid) { store?.handle(it, MESH)?.value }

  /** The material asset [guid], or null when it is missing or fails to load. */
  fun material(guid: Guid): MaterialAsset? = get(guid) { store?.handle(it, MATERIAL)?.value }

  /** Forgets the failures once the store has reloaded something since they were recorded. */
  fun retryAfterReloads() {
    val changes = store?.changes ?: return
    if (changes != seenChanges) {
      seenChanges = changes
      failed.clear()
    }
  }

  private companion object {
    /** The asset types, made once: a class literal makes a new object each time it is used. */
    val MESH: KClass<MeshAsset> = MeshAsset::class
    val MATERIAL: KClass<MaterialAsset> = MaterialAsset::class
  }

  private inline fun <T> get(guid: Guid, load: (Guid) -> T?): T? {
    if (store == null || failed.containsKey(guid)) return null
    return try {
      load(guid)
    } catch (failure: AssetLoadException) {
      failed[guid] = failure.message.orEmpty()
      null
    }
  }
}
