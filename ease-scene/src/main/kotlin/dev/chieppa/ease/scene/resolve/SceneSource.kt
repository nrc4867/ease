package dev.chieppa.ease.scene.resolve

import dev.chieppa.ease.assets.Guid
import dev.chieppa.ease.scene.document.SceneDocument

/** Where resolving finds the scenes and prefabs that placements name. */
public fun interface SceneSource {
  /** The scene or prefab [asset], or null when there is none. */
  public fun document(asset: Guid): SceneDocument?
}
