package dev.chieppa.ease.runtime

import dev.chieppa.ease.assets.reload.registerAssetCommands
import dev.chieppa.ease.commands.WorldCodec
import dev.chieppa.ease.commands.worldCodec
import dev.chieppa.ease.render.Quad
import dev.chieppa.ease.render.QuadReplicated
import dev.chieppa.ease.scene.commands.registerSceneCommands
import dev.chieppa.ease.scene.types.SceneTypes

/**
 * The types every world of [module] knows, the same on host and clients so their types hashes
 * match: the engine's commands and components, the asset and scene commands, and the module's own.
 * Each scene component type is registered once, through [scene]'s builder, which hands it to the
 * [codec] as well.
 */
internal class WorldTypes(module: GameModule) {
  /** The component types scenes save and spawn. */
  lateinit var scene: SceneTypes
    private set

  /** Reads and writes the world's commands and state. */
  val codec: WorldCodec = worldCodec {
    registerAssetCommands()
    registerSceneCommands()
    replicated(QuadReplicated)
    scene =
      SceneTypes.build(this) {
        component(Quad, Quad.serializer())
        module.registerSceneTypes(this)
      }
    module.registerTypes(this)
  }
}
