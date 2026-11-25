package dev.chieppa.ease.runtime

import dev.chieppa.ease.assets.loading.AssetStore
import dev.chieppa.ease.commands.CommandOrigin
import dev.chieppa.ease.ecs.DEFAULT_ENTITY_CAPACITY
import dev.chieppa.ease.ecs.replication.PeerId
import dev.chieppa.ease.net.command.CommandExecutor
import dev.chieppa.ease.net.join.ContentManifest
import dev.chieppa.ease.net.session.HostPeer
import dev.chieppa.ease.net.session.HostSession
import dev.chieppa.ease.net.session.NetSettings
import dev.chieppa.ease.net.transport.LoopbackLink
import dev.chieppa.ease.net.transport.LoopbackTransport
import dev.chieppa.ease.net.view.ReplicaContent
import dev.chieppa.ease.net.view.SceneReplicas
import dev.chieppa.ease.render.RenderSnapshot
import dev.chieppa.ease.runtime.loop.FixedStep

/**
 * A game as the player runs it, single-player included: the host's world, which holds authority,
 * and a local client's world joined to it over the [transport], as any other client would join.
 * Each [step] is one tick of both: the client's input goes to the host through the session, the
 * host ticks, and its changes come back as spawns and snapshots. Rendering shows the [client].
 *
 * The join handshake compares the content both build from: with [assets], every asset in its
 * database, the content a game ships; enabled mods join it once mods exist. Both worlds read the
 * same files, so they always match here. A tick allocates nothing but the commands it carries.
 */
public class LocalHost(
  private val module: GameModule,
  fixedStep: FixedStep,
  input: InputScript? = null,
  assets: AssetStore? = null,
  entityCapacity: Int = DEFAULT_ENTITY_CAPACITY,
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
    )

  /** The local client's link; its conditions simulate a network, perfect by default. */
  public val link: LoopbackLink = transport.connect(LOCAL_PEER)

  /** The local client: the world a player sees and steers. */
  public val client: ClientWorld = localClient(fixedStep, entityCapacity, input, assets)

  private val joined = ArrayList<HostPeer>()

  /** What rendering shows: the client world's extract. */
  public val snapshot: RenderSnapshot
    get() = client.world.snapshot

  /** The host's tick the next [step] runs. */
  public val tick: Long
    get() = host.tick

  /**
   * One tick: the transport moves on and the host takes new connections; the client sends its
   * commands and input; the host takes them, welcomes peers whose join it accepted, takes each
   * peer's input for the tick, ticks and sends what changed; then the client takes it and ticks.
   */
  public fun step() {
    transport.update()
    while (true) session.connect(transport.accept() ?: break)
    client.beforeHost()
    val hostTick = host.tick
    session.receive(hostTick)
    welcomeJoined()
    takeInputs(hostTick)
    host.step()
    session.send(hostTick)
    client.afterHost()
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
      if (peer.greeted && peer.isOpen) peer.inputs.take(hostTick)
    }
  }

  private fun localClient(
    fixedStep: FixedStep,
    entityCapacity: Int,
    input: InputScript?,
    assets: AssetStore?,
  ): ClientWorld {
    val world = HostedWorld(module, fixedStep, entityCapacity, LOCAL_PEER)
    val content = assets?.let { SceneReplicas(world.world, it) } ?: ReplicaContent.NONE
    return ClientWorld(world, link.clientEnd, settings, content, input)
  }

  public companion object {
    /** The local client's peer id. */
    public val LOCAL_PEER: PeerId = PeerId(1)
  }
}
