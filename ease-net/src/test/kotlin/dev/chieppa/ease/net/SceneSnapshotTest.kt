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
import dev.chieppa.ease.ecs.replication.Authority
import dev.chieppa.ease.ecs.replication.NetworkIdentity
import dev.chieppa.ease.ecs.replication.PeerId
import dev.chieppa.ease.ecs.replication.ReplicationSettings
import dev.chieppa.ease.ecs.replication.networkIdOf
import dev.chieppa.ease.net.channel.LocalLink
import dev.chieppa.ease.net.input.InputFrame
import dev.chieppa.ease.net.session.ClientSession
import dev.chieppa.ease.net.session.HostSession
import dev.chieppa.ease.net.session.NetSettings
import dev.chieppa.ease.platform.files.FileChangeKind
import dev.chieppa.ease.platform.files.ManualFileWatcher
import dev.chieppa.ease.scene.assets.registerScenes
import dev.chieppa.ease.scene.commands.registerSceneCommands
import dev.chieppa.ease.scene.document.SceneDocument
import dev.chieppa.ease.scene.document.SceneNode
import dev.chieppa.ease.scene.files.SceneFolder
import dev.chieppa.ease.scene.node.LocalId
import dev.chieppa.ease.scene.node.NodeKey
import dev.chieppa.ease.scene.node.OrderKey
import dev.chieppa.ease.scene.runtime.SceneNodes
import dev.chieppa.ease.scene.runtime.SceneSync
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.io.TempDir

/**
 * A scene reload on the host changes a node's transform through the history, and the change reaches
 * a client through the delta snapshots of a session, with the write guard on both sides.
 */
class SceneSnapshotTest {
  @TempDir lateinit var folder: Path

  private val caveGuid = Guid(0, 1)
  private val hallKey = NodeKey.of(LocalId(1))
  private val cave by lazy { folder.resolve("cave.scene") }

  private fun hall(x: Float): SceneDocument {
    val transform = JsonObject(mapOf("x" to JsonPrimitive(x)))
    val node = SceneNode(LocalId(1), "Hall", null, OrderKey.FIRST, mapOf(TRANSFORM to transform))
    return SceneDocument(nodes = listOf(node))
  }

  private fun world(replication: ReplicationSettings, store: AssetStore?): World =
    phasedWorld(replication = replication) {
      store?.let { add(Phase.INPUT, AssetReloadSystem(it)) }
      add(Phase.INPUT, SceneNodes())
    }

  @Test
  fun `a reloaded transform reaches the client through snapshots`() {
    SceneFolder.write(cave, hall(1f))
    MetaFiles.write(MetaFiles.metaOf(cave), AssetMeta(guid = caveGuid))
    val watcher = ManualFileWatcher()
    val associations = FileAssociationRegistry().apply { registerScenes() }
    val database = AssetDatabase.load(folder, watcher, associations = associations)
    val store = AssetStore(database, associations)
    val codec = worldCodec {
      registerAssetCommands()
      registerSceneCommands()
    }
    val settings = { peer: PeerId ->
      ReplicationSettings(peer, codec.replicated, debugChecks = true)
    }
    val host = world(settings(PeerId.HOST), store)
    val client = world(settings(CLIENT), null)
    val history = CommandHistory(host, codec)
    val assets = AssetSync(database, store, history)
    val scenes = SceneSync(host, store, history)
    scenes.place(caveGuid, tick = 0)
    val held = host.system<SceneNodes>().index[hallKey]
    val mirrored = mirror(host, client, held)
    val net = NetSettings()
    val link = LocalLink(clientPeer = CLIENT)
    val hostSession = HostSession(host, codec, net)
    val peer = hostSession.connect(link.hostEnd)
    val clientSession = ClientSession(client, codec, link.clientEnd, net)
    val input = InputFrame(net.input)
    var tick = 0L
    val step = {
      clientSession.sendInput(tick, input)
      hostSession.receive(tick)
      peer.inputs.take(tick)
      host.update(DELTA)
      hostSession.send(tick)
      clientSession.receive()
      client.update(DELTA)
      tick++
    }
    repeat(SETTLE) { step() }

    SceneFolder.write(cave, hall(5f)).written.forEach {
      watcher.publish(cave.resolve(it), FileChangeKind.MODIFIED)
    }
    assets.beforeTick(tick)
    scenes.beforeTick(tick)
    repeat(SETTLE) { step() }

    assertEquals(5f, with(host) { held[LocalTransform].x })
    assertEquals(5f, with(client) { mirrored[LocalTransform].x })
  }

  /** The client's copy of the host's [held] node, under the same network id, held by the host. */
  private fun mirror(host: World, client: World, held: Entity): Entity {
    val x = with(host) { held[LocalTransform].x }
    return client.entity {
      it += LocalTransform().apply { this.x = x }
      it += Authority(PeerId.HOST, PeerId.HOST)
      it += NetworkIdentity(host.networkIdOf(held))
      it += hallKey
    }
  }

  private companion object {
    val CLIENT = PeerId(1)
    const val TRANSFORM = "ease.LocalTransform"
    const val DELTA = 1f / 60f
    const val SETTLE = 5
  }
}
