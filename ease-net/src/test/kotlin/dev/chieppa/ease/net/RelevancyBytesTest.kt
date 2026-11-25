package dev.chieppa.ease.net

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import dev.chieppa.ease.commands.worldCodec
import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.ecs.phasedWorld
import dev.chieppa.ease.ecs.replication.Audience
import dev.chieppa.ease.ecs.replication.Authority
import dev.chieppa.ease.ecs.replication.PeerId
import dev.chieppa.ease.ecs.replication.ReplicationSettings
import dev.chieppa.ease.ecs.replication.entityOf
import dev.chieppa.ease.ecs.replication.networkIdOf
import dev.chieppa.ease.ecs.replication.replicatedSchema
import dev.chieppa.ease.ecs.replication.replication
import dev.chieppa.ease.ecs.replication.spawnEntity
import dev.chieppa.ease.net.relevancy.AlwaysRelevant
import dev.chieppa.ease.net.relevancy.AlwaysRelevantFilter
import dev.chieppa.ease.net.relevancy.Area
import dev.chieppa.ease.net.relevancy.AreaFilter
import dev.chieppa.ease.net.relevancy.DistanceFilter
import dev.chieppa.ease.net.relevancy.Team
import dev.chieppa.ease.net.relevancy.TeamFilter
import dev.chieppa.ease.net.relevancy.registerRelevancyComponents
import dev.chieppa.ease.net.session.ClientSession
import dev.chieppa.ease.net.session.HostSession
import dev.chieppa.ease.net.transport.LoopbackTransport
import dev.chieppa.ease.net.view.ViewSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Inspects every byte two clients receive: an entity outside a client's relevancy, and a field
 * outside its audience, never appears in them, in spawns or in snapshots, while the same kind of
 * value inside them does. Each value is a marker whose 32 bits are searched for at every bit
 * offset.
 */
class RelevancyBytesTest {
  private val badge =
    replicatedSchema("test.Badge") {
      float("shown")
      float("secret", audience = Audience.OWNER_ONLY)
      float("hidden", audience = Audience.ALL_BUT_OWNER)
    }
  private val codec = worldCodec {
    replicated(badge.serializer)
    registerRelevancyComponents()
  }
  private val host = world(PeerId.HOST)
  private val view =
    ViewSettings(listOf(DistanceFilter(RADIUS), TeamFilter, AreaFilter, AlwaysRelevantFilter))
  private val hostSession = HostSession(host, codec, viewSettings = view)

  private val near = badged(x = 1f, shown = 101.25f)
  private val far = badged(x = RADIUS * 3, shown = 102.25f)
  private val ownedByB = badged(x = 2f, shown = 103.25f, secret = 104.25f, hidden = 105.25f)
  private val teamed = badged(x = 3f, shown = 106.25f, extra = Team(TEAM))
  private val inArea = badged(x = 4f, shown = 107.25f, extra = Area(AREA))
  private val always = badged(x = RADIUS * 4, shown = 108.25f, extra = AlwaysRelevant())

  private val a = Client(PeerId(1))
  private val b = Client(PeerId(2))
  private var tick = 0L

  init {
    with(host) { ownedByB[Authority].owner = b.peer }
    a.hostPeer.viewer.team = TEAM
  }

  private inner class Client(val peer: PeerId) {
    val world = world(peer)
    private val link = LoopbackTransport().connect(peer)
    val hostPeer = hostSession.connect(link.hostEnd)
    val bytes = RecordingConnection(link.clientEnd)
    val session = ClientSession(world, codec, bytes)

    fun holds(entity: Entity): Boolean = world.entityOf(host.networkIdOf(entity)) != Entity.NONE
  }

  private fun world(peer: PeerId): World =
    phasedWorld(replication = ReplicationSettings(peer, codec.replicated, debugChecks = true)) {}

  @Suppress("LongParameterList") // Named arguments with defaults keep each entity short.
  private fun badged(
    x: Float,
    shown: Float,
    secret: Float = 0f,
    hidden: Float = 0f,
    extra: Component<out Any>? = null,
  ): Entity = host.spawnEntity {
    it += LocalTransform().apply { this.x = x }
    it +=
      badge.component().apply {
        setFloat(SHOWN, shown)
        setFloat(SECRET, secret)
        setFloat(HIDDEN, hidden)
      }
    it += listOfNotNull(extra)
  }

  private fun step() {
    hostSession.receive(tick)
    host.update(DELTA)
    hostSession.send(tick)
    a.session.receive()
    b.session.receive()
    tick++
  }

  /** Changes every badge to its marker plus [offset], so snapshots carry new values. */
  private fun rewrite(offset: Float) {
    for (entity in listOf(near, far, ownedByB, teamed, inArea, always)) {
      host.replication.write(entity, badge.type) {
        for (field in 0 until FIELDS) setFloat(field, getFloat(field) + offset)
      }
    }
  }

  private fun run() {
    repeat(SETTLE) { step() }
    rewrite(OFFSET)
    repeat(SETTLE) { step() }
  }

  @Test
  fun `a client never receives an entity outside its relevancy`() {
    run()

    for (offset in listOf(0f, OFFSET)) {
      assertTrue(a.bytes.carries(101.25f + offset), "the near entity reaches A")
      assertFalse(a.bytes.carries(102.25f + offset), "the far entity never reaches A")
      assertTrue(a.bytes.carries(106.25f + offset), "A's team's entity reaches A")
      assertFalse(b.bytes.carries(106.25f + offset), "A's team's entity never reaches B")
      assertFalse(a.bytes.carries(107.25f + offset), "an entity in another area never reaches A")
      assertTrue(a.bytes.carries(108.25f + offset), "an always relevant entity reaches A")
    }
    assertFalse(a.holds(far))
    assertFalse(b.holds(teamed))
    assertTrue(a.holds(near) && a.holds(always) && b.holds(near))
  }

  @Test
  fun `owner-only and all-but-owner fields reach only their audience`() {
    run()

    for (offset in listOf(0f, OFFSET)) {
      assertTrue(b.bytes.carries(104.25f + offset), "the owner receives its owner-only field")
      assertFalse(a.bytes.carries(104.25f + offset), "another peer never receives it")
      assertTrue(a.bytes.carries(105.25f + offset), "another peer receives all-but-owner")
      assertFalse(b.bytes.carries(105.25f + offset), "the owner never receives all-but-owner")
      assertTrue(a.bytes.carries(103.25f + offset) && b.bytes.carries(103.25f + offset))
    }
    val mirror = a.world.entityOf(host.networkIdOf(ownedByB))
    val secret = with(a.world) { (mirror[badge.type]).getFloat(SECRET) }
    assertEquals(0f, secret)
  }

  private companion object {
    const val RADIUS = 50f
    const val TEAM = 7
    const val AREA = 3
    const val SHOWN = 0
    const val SECRET = 1
    const val HIDDEN = 2
    const val FIELDS = 3
    const val OFFSET = 1000f
    const val SETTLE = 4
    const val DELTA = 1f / 60f
  }
}
