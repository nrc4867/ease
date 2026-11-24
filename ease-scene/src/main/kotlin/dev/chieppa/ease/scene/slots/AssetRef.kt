package dev.chieppa.ease.scene.slots

import dev.chieppa.ease.annotations.Doc
import dev.chieppa.ease.assets.Guid
import kotlinx.serialization.Serializable

/** The serial name of asset slots, which resolving a scene looks for. */
public const val ASSET_REF_NAME: String = "ease.AssetRef"

/**
 * An asset slot: a component field naming an asset by GUID, such as a mesh, a sound or a prefab.
 * [T] is what the slot accepts, so a slot for one kind of asset never takes another; the file holds
 * only the GUID, so moving or renaming the asset never breaks it. Spawning a scene checks that
 * every asset slot names an asset the project has.
 */
@Doc("An asset slot: the GUID of the asset it names.")
@Serializable(with = AssetRefSerializer::class)
public class AssetRef<T : Any>(@Doc("The asset's GUID.") public val guid: Guid) {
  override fun equals(other: Any?): Boolean = other is AssetRef<*> && other.guid == guid

  override fun hashCode(): Int = guid.hashCode()

  override fun toString(): String = "AssetRef($guid)"
}
