package dev.chieppa.ease.runtime

import dev.chieppa.ease.commands.Command
import dev.chieppa.ease.net.channel.Connection
import dev.chieppa.ease.net.input.InputFrame
import dev.chieppa.ease.net.session.ClientSession
import dev.chieppa.ease.net.session.NetSettings
import dev.chieppa.ease.net.view.ReplicaContent

/**
 * A client's [world] joined to its host over [connection]: the host's entities arrive as spawns and
 * snapshots, and the client's [input] script and [inputFrame] go to the host through the [session].
 * This is the world a player sees and steers. A tick allocates nothing but the commands it sends.
 */
public class ClientWorld(
  public val world: HostedWorld,
  connection: Connection,
  settings: NetSettings,
  content: ReplicaContent = ReplicaContent.NONE,
  private val input: InputScript? = null,
) {
  /** The client's side of the session with the host. */
  public val session: ClientSession =
    ClientSession(world.world, world.codec, connection, settings, content)

  /**
   * The input the client sends with its next tick; whatever fills it (devices, a recording) sets it
   * before the tick.
   */
  public val inputFrame: InputFrame = InputFrame(settings.input)

  /** The client's tick the next step runs. */
  public val tick: Long
    get() = world.tick

  /** Sends [command] to the host, stamped with the client's [tick], for it to check and apply. */
  public fun send(command: Command) {
    session.sendCommand(world.tick, command)
  }

  /** Before the host's tick: the input script's commands, then this tick's input. */
  internal fun beforeHost() {
    input?.send(this)
    session.sendInput(world.tick, inputFrame)
  }

  /** After the host's tick: takes what the host sent, then steps the client's world. */
  internal fun afterHost() {
    session.receive()
    world.step()
  }
}
