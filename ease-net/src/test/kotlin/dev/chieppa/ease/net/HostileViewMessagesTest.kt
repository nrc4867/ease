package dev.chieppa.ease.net

import com.github.quillraven.fleks.World
import dev.chieppa.ease.assets.Guid
import dev.chieppa.ease.commands.WorldCodec
import dev.chieppa.ease.ecs.replication.BitWriter
import dev.chieppa.ease.ecs.replication.NetworkId
import dev.chieppa.ease.ecs.replication.PeerId
import dev.chieppa.ease.ecs.replication.spawnEntity
import dev.chieppa.ease.net.channel.ChannelKind
import dev.chieppa.ease.net.channel.Channels
import dev.chieppa.ease.net.join.ContentManifest
import dev.chieppa.ease.net.session.ClientSession
import dev.chieppa.ease.net.session.CloseReason
import dev.chieppa.ease.net.session.JoinHello
import dev.chieppa.ease.net.transport.LoopbackTransport
import dev.chieppa.ease.net.view.ReplicaContent
import dev.chieppa.ease.net.view.SceneReplicas
import dev.chieppa.ease.net.view.ViewRefusal
import dev.chieppa.ease.scene.document.SceneDocument
import dev.chieppa.ease.scene.document.SceneNode
import dev.chieppa.ease.scene.node.LocalId
import dev.chieppa.ease.scene.node.OrderKey
import java.nio.file.Path
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.io.TempDir

/**
 * Every spawn and despawn a client receives is hostile: random bytes, every truncation of a real
 * message and bit flips in one are applied or refused with a typed reason, never thrown past the
 * session, and a refused one changes nothing in the world. The generator is seeded.
 */
class HostileViewMessagesTest {
  private val random = Random(SEED)

  /** The view messages after the host's hello that [recorder] received. */
  private fun viewMessages(recorder: RecordingConnection): List<ByteArray> {
    val reliable = recorder.received.filter { it.first == ChannelKind.RELIABLE_ORDERED }
    return reliable.map { it.second }.drop(1)
  }

  private fun variants(real: ByteArray): List<ByteArray> {
    val cuts = (0 until real.size).map { real.copyOf(it) }
    val flips =
      List(FLIPS) {
        val bytes = real.copyOf()
        repeat(1 + random.nextInt(MAX_FLIPS)) {
          val index = random.nextInt(bytes.size)
          bytes[index] = (bytes[index].toInt() xor (1 shl random.nextInt(Byte.SIZE_BITS))).toByte()
        }
        bytes
      }
    val noise = List(NOISE) { random.nextBytes(1 + random.nextInt(MAX_NOISE)) }
    return cuts + flips + noise
  }

  /** Feeds [bytes] as a view message to a fresh client session over [world]. */
  private fun feed(
    world: World,
    codec: WorldCodec,
    content: ReplicaContent,
    bytes: ByteArray,
  ): ClientSession {
    val link = LoopbackTransport().connect(Session.CLIENT)
    val client = ClientSession(world, codec, link.clientEnd, content = content)
    JoinHello.send(link.hostEnd, codec.typesHash, ContentManifest.EMPTY)
    link.hostEnd.send(ChannelKind.RELIABLE_ORDERED, bytes, 0, bytes.size)
    val before = world.numEntities
    if (!client.receive()) {
      assertTrue(client.closeReason in REFUSALS, client.closeReason.toString())
      assertTrue(client.viewRefusal != null)
      assertEquals(before, world.numEntities)
    }
    return client
  }

  /** A plain spawn of [id] with no components, owned by the raw ten groups of [ownerGroups]. */
  private fun plainSpawn(id: Long, ownerGroups: IntArray): ByteArray {
    val writer = BitWriter(Channels.MAX_RELIABLE_BYTES)
    writer.writeBits(SPAWN, Byte.SIZE_BITS)
    writer.writeVarLong(id)
    ownerGroups.forEachIndexed { index, group ->
      writer.writeBits(group, GROUP_BITS)
      writer.writeBoolean(index < ownerGroups.size - 1)
    }
    writer.writeBits(0, Byte.SIZE_BITS)
    writer.writeBits(0, COUNT_BITS)
    writer.padToByte()
    return writer.buffer.copyOf(writer.byteLength)
  }

  @Test
  fun `a spawn whose owner decodes below 0 is refused`() {
    val session = Session(entities = 1)
    val (target, _) = session.lateClient(PeerId(2))
    val negative = IntArray(MAX_GROUPS) { if (it < MAX_GROUPS - 1) GROUP_MASK else 1 }

    val client = feed(target, session.codec, ReplicaContent.NONE, plainSpawn(1, negative))

    assertEquals(ViewRefusal.BAD_PEER, client.viewRefusal)
  }

  @Test
  fun `a spawn naming an id from a client's own range is refused`() {
    val session = Session(entities = 1)
    val (target, _) = session.lateClient(PeerId(2))

    val spawn = plainSpawn(NetworkId.LOCAL_START, intArrayOf(1))
    val client = feed(target, session.codec, ReplicaContent.NONE, spawn)

    assertEquals(ViewRefusal.BAD_NETWORK_ID, client.viewRefusal)
  }

  @Test
  fun `hostile plain spawns and despawns never throw, and a refused one changes nothing`() {
    var recorder: RecordingConnection? = null
    val session = Session(entities = 2) { RecordingConnection(it).also { r -> recorder = r } }
    session.host.spawnEntity()
    session.step()
    session.host -= session.held[1]
    session.step()
    val messages = viewMessages(checkNotNull(recorder))
    val (target, _) = session.lateClient(PeerId(2))

    messages.forEach { real ->
      variants(real).forEach { feed(target, session.codec, ReplicaContent.NONE, it) }
    }
  }

  @Test
  fun `hostile scene node spawns never throw, and a refused one changes nothing`(
    @TempDir folder: Path
  ) {
    val node = SceneNode(LocalId(1), "Hall", null, OrderKey.FIRST, emptyMap())
    ScenePair.save(folder.resolve("cave.scene"), SceneDocument(nodes = listOf(node)), Guid(0, 1))
    var recorder: RecordingConnection? = null
    val pair = ScenePair(folder) { RecordingConnection(it).also { r -> recorder = r } }
    pair.scenes.place(Guid(0, 1), tick = 0)
    pair.step()
    val real = viewMessages(checkNotNull(recorder)).single()
    val target = pair.Peer(PeerId(2), folder)
    val content = SceneReplicas(target.world, target.store)

    variants(real).forEach { feed(target.world, pair.codec, content, it) }
  }

  private companion object {
    const val SEED = 7_331L
    const val FLIPS = 1_000
    const val MAX_FLIPS = 4
    const val NOISE = 1_000
    const val MAX_NOISE = 300
    const val SPAWN = 1
    const val GROUP_BITS = 7
    const val GROUP_MASK = 0x7F
    const val MAX_GROUPS = 10
    const val COUNT_BITS = 9
    val REFUSALS = setOf(CloseReason.MALFORMED_SPAWN, CloseReason.CONTENT_MISMATCH)
  }
}
