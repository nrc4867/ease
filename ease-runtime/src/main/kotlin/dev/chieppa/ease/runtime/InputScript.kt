package dev.chieppa.ease.runtime

/**
 * Scripted input: what a client gives before each of its ticks, as a recorded or scripted device
 * would, for demos and headless runs: the tick's [ClientWorld.inputFrame], and commands. It reads
 * the client's world and changes the host's only through that input and [ClientWorld.send].
 */
public fun interface InputScript {
  /** Sets the input and sends the commands due before [client]'s next tick, [ClientWorld.tick]. */
  public fun send(client: ClientWorld)
}
