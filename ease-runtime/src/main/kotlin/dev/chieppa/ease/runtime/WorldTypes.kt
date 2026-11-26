package dev.chieppa.ease.runtime

import dev.chieppa.ease.assets.reload.registerAssetCommands
import dev.chieppa.ease.commands.WorldCodec
import dev.chieppa.ease.commands.worldCodec
import dev.chieppa.ease.render.scene.Camera
import dev.chieppa.ease.render.scene.CameraReplicated
import dev.chieppa.ease.render.scene.DirectionalLight
import dev.chieppa.ease.render.scene.DirectionalLightReplicated
import dev.chieppa.ease.render.scene.Material
import dev.chieppa.ease.render.scene.MaterialRef
import dev.chieppa.ease.render.scene.MaterialReplicated
import dev.chieppa.ease.render.scene.Mesh
import dev.chieppa.ease.render.scene.MeshRef
import dev.chieppa.ease.render.scene.MeshReplicated
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
    replicated(MeshReplicated)
    replicated(MaterialReplicated)
    replicated(CameraReplicated)
    replicated(DirectionalLightReplicated)
    scene =
      SceneTypes.build(this) {
        component(Mesh, Mesh.serializer())
        component(Material, Material.serializer())
        component(MeshRef, MeshRef.serializer())
        component(MaterialRef, MaterialRef.serializer())
        component(Camera, Camera.serializer())
        component(DirectionalLight, DirectionalLight.serializer())
        module.registerSceneTypes(this)
      }
    module.registerTypes(this)
  }
}
