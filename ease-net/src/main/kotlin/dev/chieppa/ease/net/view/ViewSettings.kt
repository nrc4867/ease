package dev.chieppa.ease.net.view

import dev.chieppa.ease.net.relevancy.DistancePriority
import dev.chieppa.ease.net.relevancy.PriorityRule
import dev.chieppa.ease.net.relevancy.RelevancyFilter

/**
 * What the host sends each peer: the [filters] that decide which entities are relevant to it, how
 * many ticks [graceTicks] an entity out of range stays in view before it leaves, so one at the edge
 * does not spawn and despawn tick after tick, the [priority] that orders a full snapshot, and the
 * [naming] that says what asset a spawn names.
 */
public class ViewSettings(
  public val filters: List<RelevancyFilter> = emptyList(),
  public val graceTicks: Int = DEFAULT_GRACE_TICKS,
  public val priority: PriorityRule = DistancePriority(),
  public val naming: SpawnNaming = SpawnNaming.PLAIN,
) {
  init {
    require(graceTicks >= 0) { "Grace is 0 ticks or more, not $graceTicks" }
  }

  public companion object {
    /** Ticks an entity stays in view after it stops being relevant: half a second at 60 Hz. */
    public const val DEFAULT_GRACE_TICKS: Int = 30
  }
}
