package dev.chieppa.ease.runtime

/**
 * Scripted input: commands a client sends the host before each of its ticks, as a recorded or
 * scripted device would, for demos and headless runs. It reads the client's world and changes the
 * host's only through [ClientWorld.send].
 */
public fun interface InputScript {
  /** Sends the commands due before [client]'s next tick, [ClientWorld.tick]. */
  public fun send(client: ClientWorld)
}
