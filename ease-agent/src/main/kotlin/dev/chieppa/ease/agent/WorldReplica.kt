package dev.chieppa.ease.agent

import dev.chieppa.ease.commands.CommandLogEntry
import dev.chieppa.ease.commands.CommandRejected
import dev.chieppa.ease.runtime.HostedWorld

/**
 * Reproduces a live world in a scratch one, the live one only read: a fresh world from the same
 * module, whose own starting commands must match the start of the live log, then every later logged
 * command applied at its tick while the scratch world is stepped, up to the live tick. Any
 * difference left is a change the log does not hold. Rejected commands are recorded in [log].
 */
public class WorldReplica(private val world: AgentWorld, private val log: HostLog) {
  /** A scratch copy of [live] at its tick. Throws [ReplayFailure] when the log cannot get there. */
  public fun of(live: HostedWorld): HostedWorld {
    val scratch = world.fresh()
    val entries = live.history.log.all
    val seeded = scratch.history.log.all
    checkStart(seeded, entries)
    for (index in seeded.size until entries.size) {
      val entry = entries[index]
      while (scratch.tick < entry.tick) scratch.step()
      try {
        scratch.execute(live.history.log.commandOf(entry), entry.origin)
      } catch (rejected: CommandRejected) {
        val reason = "Logged command $index at tick ${entry.tick} was rejected: ${rejected.message}"
        log.record(reason)
        throw ReplayFailure(reason)
      }
    }
    while (scratch.tick < live.tick) scratch.step()
    return scratch
  }

  /** Fails unless the [logged] commands start with exactly the game's own starting commands. */
  private fun checkStart(own: List<CommandLogEntry>, logged: List<CommandLogEntry>) {
    val first =
      own.indices.firstOrNull { index ->
        val entry = logged.getOrNull(index)
        entry == null ||
          own[index].tick != entry.tick ||
          !own[index].bytes().contentEquals(entry.bytes())
      } ?: return
    throw ReplayFailure("The game's starting command $first is not the logged one")
  }
}
