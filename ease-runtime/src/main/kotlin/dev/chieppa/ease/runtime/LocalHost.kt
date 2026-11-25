package dev.chieppa.ease.runtime

import dev.chieppa.ease.assets.loading.AssetStore
import dev.chieppa.ease.commands.CommandOrigin
import dev.chieppa.ease.ecs.DEFAULT_ENTITY_CAPACITY
import dev.chieppa.ease.ecs.prediction.CorrectionOffsets
import dev.chieppa.ease.ecs.replication.PeerId
import dev.chieppa.ease.net.command.CommandExecutor
import dev.chieppa.ease.net.join.ContentManifest
import dev.chieppa.ease.net.prediction.PredictionSettings
import dev.chieppa.ease.net.session.HostPeer
import dev.chieppa.ease.net.session.HostSession
import dev.chieppa.ease.net.session.NetSettings
import dev.chieppa.ease.net.transport.LoopbackLink
import dev.chieppa.ease.net.transport.LoopbackTransport
import dev.chieppa.ease.net.view.ReplicaContent
import dev.chieppa.ease.net.view.SceneNaming
import dev.chieppa.ease.net.view.SceneReplicas
import dev.chieppa.ease.net.view.SpawnNaming
import dev.chieppa.ease.net.view.ViewSettings
import dev.chieppa.ease.render.RenderSnapshot
import dev.chieppa.ease.runtime.loop.FixedStep

/**
 * A game as the player runs it, single-player included: the host's world, which holds authority,
 * and a local client's world joined to it over the [transport], as any other client would join.
 * Each [step] is one tick of the host: each client takes what the host sent and runs its own tick
 * (none or two while its clock adjusts), predicting from its input, which goes to the host through
 * the session; then the host ticks and sends its changes as spawns and snapshots. Clients predict
 * and interpolate as [prediction] says. Rendering shows the [client].
 *
 * With [assets], the game's content (see [GameContent]), the host places the game's start scene and
 * keeps it in step with its files, and names each scene node it spawns by its scene and key; each
 * client builds those nodes from the same files, and the host's snapshots carry the rest. The join
 * handshake compares the content both build from: every asset in the database, the content a game
 * ships; enabled mods join it once mods exist. Both worlds read the same files, so they always
 * match here. A tick allocates nothing but the commands it carries.
 */
public class LocalHost(
  private val module: GameModule,
  private val fixedStep: FixedStep,
  input: InputScript? = null,
  private val assets: AssetStore? = null,
  private val entityCapacity: Int = DEFAULT_ENTITY_CAPACITY,
  private val prediction: PredictionSettings = PredictionSettings(),
) {
  /** The in-process transport between the host and the local client. */
  public val transport: LoopbackTransport = LoopbackTransport()

  /** The host's world: authority over everything the game made. */
  public val host: HostedWorld = HostedWorld(module, fixedStep, entityCapacity, assets = assets)

  /** What host and client agree on, the content they build from among it. */
  public val settings: NetSettings =
    NetSettings(content = ContentManifest.of(assets?.database?.assets.orEmpty()))

  /** The host's side of the session, taking commands from peers into the host's history. */
  public val session: HostSession =
    HostSession(
      host.world,
      host.codec,
      settings,
      CommandExecutor { command, _, tick ->
        host.history.execute(command, tick, CommandOrigin.NETWORK)
      },
      viewSettings = ViewSettings(naming = host.scenes?.let(::SceneNaming) ?: SpawnNaming.PLAIN),
    )

  /** The local client's link; its conditions simulate a network, perfect by default. */
  public val link: LoopbackLink = transport.connect(LOCAL_PEER)

  private val joined = ArrayList<HostPeer>()
  private val joinedClients = ArrayList<ClientWorld>()

  /** The local client: the world a player sees and steers. */
  public val client: ClientWorld = clientOver(LOCAL_PEER, link, input)

  /** Every client in this process, the local one first. */
  public val clients: List<ClientWorld>
    get() = joinedClients

  /** What rendering shows: the client world's extract. */
  public val snapshot: RenderSnapshot
    get() = client.world.snapshot

  /** The host's tick the next [step] runs. */
  public val tick: Long
    get() = host.tick

  /**
   * One tick: the transport moves on and the host takes new connections; each client takes what the
   * host sent and runs its ticks, sending its commands and input; the host takes them, welcomes
   * peers whose join it accepted, takes each peer's input for the tick, ticks and sends what
   * changed.
   */
  public fun step() {
    transport.update()
    while (true) session.connect(transport.accept() ?: break)
    for (index in joinedClients.indices) joinedClients[index].step()
    val hostTick = host.tick
    session.receive(hostTick)
    welcomeJoined()
    takeInputs(hostTick)
    host.step()
    session.send(hostTick)
  }

  /**
   * Joins another client in this process as [peer], with its own [input] script, as editor
   * multi-client play does; it takes part from the next [step]. Throws when [peer] is the host or
   * already joined.
   */
  public fun addClient(peer: PeerId, input: InputScript? = null): ClientWorld {
    require(peer != PeerId.HOST) { "The host's peer id is not a client's" }
    require(joinedClients.none { it.world.peer == peer }) { "A client already joined as $peer" }
    return clientOver(peer, transport.connect(peer), input)
  }

  private fun welcomeJoined() {
    val peers = session.peers
    for (index in peers.indices) {
      val peer = peers[index]
      if (peer.greeted && peer.isOpen && peer !in joined) {
        joined += peer
        module.peerJoined(host, peer.peer)
      }
    }
  }

  private fun takeInputs(hostTick: Long) {
    val peers = session.peers
    for (index in peers.indices) {
      val peer = peers[index]
      when {
        peer.greeted && peer.isOpen -> host.inputs[peer.peer] = peer.inputs.take(hostTick)
        !peer.isOpen -> host.inputs.remove(peer.peer)
      }
    }
  }

  private fun clientOver(peer: PeerId, link: LoopbackLink, input: InputScript?): ClientWorld {
    val corrections = CorrectionOffsets(prediction.smoothingKeep, prediction.snapDistance)
    val world = HostedWorld(module, fixedStep, entityCapacity, peer, corrections = corrections)
    val content = assets?.let { SceneReplicas(world.world, it) } ?: ReplicaContent.NONE
    val client = ClientWorld(world, link.clientEnd, settings, content, input, prediction)
    joinedClients += client
    return client
  }

  public companion object {
    /** The local client's peer id. */
    public val LOCAL_PEER: PeerId = PeerId(1)
  }
}
