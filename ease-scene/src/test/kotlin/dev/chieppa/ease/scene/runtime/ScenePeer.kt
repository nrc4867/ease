package dev.chieppa.ease.scene.runtime

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import dev.chieppa.ease.assets.associations.FileAssociationRegistry
import dev.chieppa.ease.assets.database.AssetDatabase
import dev.chieppa.ease.assets.loading.AssetStore
import dev.chieppa.ease.assets.reload.AssetReloadSystem
import dev.chieppa.ease.assets.reload.AssetSync
import dev.chieppa.ease.assets.reload.registerAssetCommands
import dev.chieppa.ease.commands.CommandHistory
import dev.chieppa.ease.commands.CommandOrigin
import dev.chieppa.ease.commands.worldCodec
import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.ecs.Phase
import dev.chieppa.ease.ecs.phasedWorld
import dev.chieppa.ease.ecs.replication.PeerId
import dev.chieppa.ease.ecs.replication.ReplicationSettings
import dev.chieppa.ease.platform.files.ManualFileWatcher
import dev.chieppa.ease.scene.Beacon
import dev.chieppa.ease.scene.Scenes
import dev.chieppa.ease.scene.assets.registerScenes
import dev.chieppa.ease.scene.commands.registerSceneCommands
import dev.chieppa.ease.scene.node.NodeKey
import java.nio.file.Path

/**
 * One peer running a world over a project folder, wired as a host wires it: the asset database and
 * store with scenes registered, asset and scene systems, the history, and the per-tick asset and
 * scene syncs. Commands another peer logged can be applied here as received from the network.
 */
class ScenePeer(root: Path, peer: PeerId = PeerId.HOST) {
  val watcher = ManualFileWatcher()
  private val associations = FileAssociationRegistry().apply { registerScenes() }
  val database: AssetDatabase = AssetDatabase.load(root, watcher, associations = associations)
  val store = AssetStore(database, associations)
  val codec = worldCodec {
    registerAssetCommands()
    registerSceneCommands()
    component(Beacon::class, Beacon.serializer())
  }
  val world: World =
    phasedWorld(
      replication = ReplicationSettings(localPeer = peer, serializers = codec.replicated)
    ) {
      add(Phase.INPUT, AssetReloadSystem(store))
      add(Phase.INPUT, SceneNodes(Scenes.types))
    }
  val history = CommandHistory(world, codec)
  private val assetSync = AssetSync(database, store, history)
  val scenes = SceneSync(world, store, history)
  private val index = world.system<SceneNodes>().index
  private var forwarded = 0

  /** The tick boundary: asset reloads, then scene changes. */
  fun beforeTick(tick: Long) {
    assetSync.beforeTick(tick)
    scenes.beforeTick(tick)
  }

  /** Applies every command [host] logged since the last call, as received from the network. */
  fun receiveFrom(host: ScenePeer) {
    val entries = host.history.log.all
    while (forwarded < entries.size) {
      val entry = entries[forwarded++]
      history.execute(host.history.log.commandOf(entry), entry.tick, CommandOrigin.NETWORK)
    }
  }

  /** The live entity of the node with [key], or [Entity.NONE]. */
  fun node(key: NodeKey): Entity = index[key]

  /** The keys of every live node, as text, sorted. */
  fun keys(): List<String> = index.keys.map { it.text }.sorted()

  /** The x of the node [key]'s transform. */
  fun x(key: NodeKey): Float = with(world) { node(key)[LocalTransform].x }
}
