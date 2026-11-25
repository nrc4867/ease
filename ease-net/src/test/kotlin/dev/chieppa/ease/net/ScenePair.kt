package dev.chieppa.ease.net

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import dev.chieppa.ease.assets.Guid
import dev.chieppa.ease.assets.associations.FileAssociationRegistry
import dev.chieppa.ease.assets.database.AssetDatabase
import dev.chieppa.ease.assets.loading.AssetStore
import dev.chieppa.ease.assets.meta.AssetMeta
import dev.chieppa.ease.assets.meta.MetaFiles
import dev.chieppa.ease.assets.reload.AssetReloadSystem
import dev.chieppa.ease.assets.reload.AssetSync
import dev.chieppa.ease.assets.reload.registerAssetCommands
import dev.chieppa.ease.commands.CommandHistory
import dev.chieppa.ease.commands.worldCodec
import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.ecs.Phase
import dev.chieppa.ease.ecs.phasedWorld
import dev.chieppa.ease.ecs.replication.PeerId
import dev.chieppa.ease.ecs.replication.ReplicationSettings
import dev.chieppa.ease.net.channel.Connection
import dev.chieppa.ease.net.input.InputFrame
import dev.chieppa.ease.net.session.ClientSession
import dev.chieppa.ease.net.session.HostSession
import dev.chieppa.ease.net.session.NetSettings
import dev.chieppa.ease.net.transport.LoopbackTransport
import dev.chieppa.ease.net.view.SceneNaming
import dev.chieppa.ease.net.view.SceneReplicas
import dev.chieppa.ease.net.view.ViewSettings
import dev.chieppa.ease.platform.files.FileChangeKind
import dev.chieppa.ease.platform.files.ManualFileWatcher
import dev.chieppa.ease.scene.assets.registerScenes
import dev.chieppa.ease.scene.commands.registerSceneCommands
import dev.chieppa.ease.scene.document.SceneDocument
import dev.chieppa.ease.scene.files.SceneFolder
import dev.chieppa.ease.scene.node.NodeKey
import dev.chieppa.ease.scene.runtime.SceneNodes
import dev.chieppa.ease.scene.runtime.SceneSync
import java.nio.file.Path

/**
 * A host over the project folder [root] and a client over [clientRoot], each with its own asset
 * database, store and scene nodes, joined by a [LoopbackTransport]: the host places scenes with
 * [SceneSync] and names its nodes by asset and key, and the client builds them from its own copy of
 * the files.
 */
internal class ScenePair(
  root: Path,
  clientRoot: Path = root,
  clientEnd: (Connection) -> Connection = { it },
) {
  val codec = worldCodec {
    registerAssetCommands()
    registerSceneCommands()
  }
  private val net = NetSettings()
  val host = Peer(PeerId.HOST, root)
  val client = Peer(CLIENT, clientRoot)
  val scenes = SceneSync(host.world, host.store, host.history)
  val link = LoopbackTransport().connect(CLIENT)
  val hostSession =
    HostSession(host.world, codec, net, viewSettings = ViewSettings(naming = SceneNaming(scenes)))
  val peer = hostSession.connect(link.hostEnd)
  val clientSession =
    ClientSession(
      client.world,
      codec,
      clientEnd(link.clientEnd),
      net,
      SceneReplicas(client.world, client.store),
    )
  private val input = InputFrame(net.input)
  var tick = 0L
    private set

  inner class Peer(peer: PeerId, root: Path) {
    val watcher = ManualFileWatcher()
    private val associations = FileAssociationRegistry().apply { registerScenes() }
    val database: AssetDatabase = AssetDatabase.load(root, watcher, associations = associations)
    val store = AssetStore(database, associations)
    val world: World =
      phasedWorld(replication = ReplicationSettings(peer, codec.replicated, debugChecks = true)) {
        add(Phase.INPUT, AssetReloadSystem(store))
        add(Phase.INPUT, SceneNodes())
      }
    val history = CommandHistory(world, codec)
    val assets = AssetSync(database, store, history)

    /** The live node with [key], or [Entity.NONE]. */
    fun node(key: NodeKey): Entity = world.system<SceneNodes>().index[key]

    /** The keys of every live node, as text, sorted. */
    fun keys(): List<String> = index().keys.map { it.text }.sorted()

    private fun index() = world.system<SceneNodes>().index

    /** The x of the node [key]'s transform. */
    fun x(key: NodeKey): Float = with(world) { node(key)[LocalTransform].x }
  }

  /** Writes [document] to the folder [path] and tells both peers' watchers, as a shared edit. */
  fun edit(path: Path, document: SceneDocument) {
    val saved = SceneFolder.write(path, document)
    (saved.written + saved.deleted).forEach { file ->
      for (peer in listOf(host, client)) peer.watcher.publish(
        path.resolve(file),
        FileChangeKind.MODIFIED,
      )
    }
  }

  /**
   * One tick: each peer's assets and the host's scenes catch up with the files, then the session.
   */
  fun step() {
    host.assets.beforeTick(tick)
    client.assets.beforeTick(tick)
    scenes.beforeTick(tick)
    clientSession.sendInput(tick, input)
    hostSession.receive(tick)
    peer.inputs.take(tick)
    host.world.update(DELTA)
    hostSession.send(tick)
    clientSession.receive()
    client.world.update(DELTA)
    tick++
  }

  companion object {
    val CLIENT = PeerId(1)

    /** Writes [document] as the scene or prefab folder [path] with [guid]. */
    fun save(path: Path, document: SceneDocument, guid: Guid) {
      SceneFolder.write(path, document)
      MetaFiles.write(MetaFiles.metaOf(path), AssetMeta(guid = guid))
    }

    const val DELTA = 1f / 60f
  }
}
