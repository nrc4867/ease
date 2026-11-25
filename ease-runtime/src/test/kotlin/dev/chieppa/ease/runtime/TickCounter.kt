package dev.chieppa.ease.runtime

import com.github.quillraven.fleks.IntervalSystem
import dev.chieppa.ease.ecs.replication.PeerId
import dev.chieppa.ease.ecs.replication.replication

/** Counts, in [ticks], the ticks of each world it runs in, by the world's own peer. */
internal class TickCounter(private val ticks: MutableMap<PeerId, Int>) : IntervalSystem() {
  private val peer = world.replication.localPeer

  override fun onTick() {
    ticks[peer] = (ticks[peer] ?: 0) + 1
  }
}
