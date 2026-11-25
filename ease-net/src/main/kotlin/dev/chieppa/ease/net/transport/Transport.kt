package dev.chieppa.ease.net.transport

import dev.chieppa.ease.net.channel.Connection

/**
 * Where a host's [Connection]s come from: the loopback transport in one process, or a socket
 * library between processes. A host [accept]s the connections clients opened and calls [update]
 * once per tick, before its sessions receive, so what arrived since the last tick is waiting on the
 * connections. How a client opens a connection depends on the transport (a peer id in the same
 * process, an address, an invite), so each transport offers its own way.
 */
public interface Transport : AutoCloseable {
  /** The next connection a client opened that the host has not taken yet, or null. */
  public fun accept(): Connection?

  /**
   * Moves the transport on one tick: what is due arrives on its connections. Allocates nothing once
   * its buffers have grown.
   */
  public fun update()

  /** Closes every connection the transport handed out. */
  override fun close()
}
