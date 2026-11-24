package dev.chieppa.ease.runtime

/**
 * Scripted input: commands a host sends a world before each tick, as a recorded or scripted device
 * would, for demos and headless runs. It changes the world only through [HostedWorld.execute].
 */
public fun interface InputScript {
  /** Sends the commands due before [world]'s next tick, [HostedWorld.tick]. */
  public fun send(world: HostedWorld)
}
