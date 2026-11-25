package dev.chieppa.ease.net

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import dev.chieppa.ease.commands.worldCodec
import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.ecs.phasedWorld
import dev.chieppa.ease.ecs.replication.Authority
import dev.chieppa.ease.ecs.replication.PeerId
import dev.chieppa.ease.ecs.replication.ReplicationSettings
import dev.chieppa.ease.ecs.replication.entityOf
import dev.chieppa.ease.ecs.replication.networkIdOf
import dev.chieppa.ease.ecs.replication.replication
import dev.chieppa.ease.ecs.replication.spawnEntity
import dev.chieppa.ease.net.channel.Connection
import dev.chieppa.ease.net.command.CommandExecutor
import dev.chieppa.ease.net.command.CommandLayer
import dev.chieppa.ease.net.command.CommandRefusals
import dev.chieppa.ease.net.input.InputFrame
import dev.chieppa.ease.net.session.ClientSession
import dev.chieppa.ease.net.session.HostPeer
import dev.chieppa.ease.net.session.HostSession
import dev.chieppa.ease.net.session.NetSettings
import dev.chieppa.ease.net.transport.LoopbackTransport
import dev.chieppa.ease.net.view.ViewSettings

/**
 * A host world holding [entities] entities with a transform, the client owning the first [owned],
 * and an empty client world connected to it over a [LoopbackTransport]: the host's spawns bring the
 * entities into the client's world. [clientEnd] may wrap the client's end, to lose or reorder what
 * it receives, and the [link]'s conditions may simulate a worse network.
 */
internal class Session(
  entities: Int,
  owned: Int = entities,
  val settings: NetSettings = NetSettings(),
  executor: CommandExecutor? = null,
  view: ViewSettings = ViewSettings(),
  clientEnd: (Connection) -> Connection = { it },
) {
  val codec = worldCodec {
    command(Push::class, Push.serializer())
    command(Note::class, Note.serializer())
  }
  val host: World = world(PeerId.HOST)
  val client: World = world(CLIENT)
  val refused = mutableListOf<Pair<CommandLayer, String>>()
  val transport = LoopbackTransport(seed = SEED)
  val link = transport.connect(CLIENT)
  private val refusals = CommandRefusals { peer, layer, reason ->
    check(peer == CLIENT)
    refused += layer to reason
  }
  val hostSession =
    if (executor == null) {
      HostSession(host, codec, settings, refusals = refusals, viewSettings = view)
    } else {
      HostSession(host, codec, settings, executor, refusals, view)
    }
  val held: List<Entity> =
    List(entities) { index ->
      val owner = if (index < owned) CLIENT else PeerId.HOST
      host.spawnEntity {
        it += LocalTransform()
        it += Authority(PeerId.HOST, owner)
      }
    }

  val peer: HostPeer = hostSession.connect(link.hostEnd)
  val clientSession = ClientSession(client, codec, clientEnd(link.clientEnd), settings)
  val input = InputFrame(settings.input)

  /** The next tick [step] runs. */
  var tick = 0L
    private set

  /**
   * One tick: the transport moves on, the client sends input, the host takes it and steps, then
   * sends snapshots.
   */
  fun step() {
    transport.update()
    clientSession.sendInput(tick, input)
    hostSession.receive(tick)
    peer.inputs.take(tick)
    host.update(DELTA)
    hostSession.send(tick)
    clientSession.receive()
    client.update(DELTA)
    tick++
  }

  /** The network id of the [index]th entity. */
  fun idOf(index: Int) = host.networkIdOf(held[index])

  /** The client's copy of the [index]th entity, or [Entity.NONE] while it has none. */
  fun mirror(index: Int): Entity = client.entityOf(idOf(index))

  /** Moves the host's [index]th entity to [x], as the host's own systems would. */
  fun moveOnHost(index: Int, x: Float) {
    host.replication.write(held[index], LocalTransform) { this.x = x }
  }

  /** The [index]th entity's x on the host and on the client. */
  fun xs(index: Int): Pair<Float, Float> =
    with(host) { held[index][LocalTransform].x } to with(client) { mirror(index)[LocalTransform].x }

  /** Every entity's transform fields, host and client, as text to compare whole worlds. */
  fun states(): Pair<String, String> = describe(host, held) to describe(client, held, ::mirror)

  /** A world for another client, [peer], and a way to describe it as [states] describes others. */
  fun lateClient(peer: PeerId): Pair<World, () -> String> {
    val world = world(peer)
    return world to { describe(world, held) { world.entityOf(idOf(it)) } }
  }

  private fun describe(
    world: World,
    entities: List<Entity>,
    find: (Int) -> Entity = { entities[it] },
  ): String =
    entities.indices.joinToString { index ->
      val entity = find(index)
      if (entity == Entity.NONE) "missing"
      else {
        val t = with(world) { entity[LocalTransform] }
        "${t.x},${t.y},${t.z},${t.rotationW},${t.scaleX}"
      }
    }

  private fun world(peer: PeerId): World =
    phasedWorld(replication = ReplicationSettings(peer, codec.replicated, debugChecks = true)) {}

  companion object {
    val CLIENT = PeerId(1)
    const val DELTA = 1f / 60f
    const val SEED = 7L
  }
}
