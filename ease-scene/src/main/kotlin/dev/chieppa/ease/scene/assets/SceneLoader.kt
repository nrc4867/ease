package dev.chieppa.ease.scene.assets

import dev.chieppa.ease.assets.Guid
import dev.chieppa.ease.assets.folders.FolderPack
import dev.chieppa.ease.assets.loading.AssetLoader
import dev.chieppa.ease.scene.document.SceneDocument
import dev.chieppa.ease.scene.files.SceneFiles
import kotlin.reflect.KClass

/** Loads a scene or prefab folder, read as its folder pack, into a [SceneDocument]. */
public object SceneLoader : AssetLoader<SceneDocument> {
  override val type: KClass<SceneDocument> = SceneDocument::class

  override fun load(guid: Guid, bytes: ByteArray): SceneDocument =
    SceneFiles.decode(FolderPack.unpack(bytes).mapValues { it.value.toString(Charsets.UTF_8) })
}
