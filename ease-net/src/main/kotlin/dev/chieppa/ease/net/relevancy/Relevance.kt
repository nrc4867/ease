package dev.chieppa.ease.net.relevancy

/** What one [RelevancyFilter] says about an entity for one viewer. */
public enum class Relevance {
  /** Relevant whatever the other filters say, such as an entity marked always relevant. */
  ALWAYS,

  /** No objection: relevant unless another filter says [NEVER]. */
  PASS,

  /**
   * Out of range, such as too far away: not relevant unless another filter says [ALWAYS], but an
   * entity in view stays for the grace ticks first, so one at the edge does not flap.
   */
  DISTANT,

  /**
   * Not relevant, unless another filter says [ALWAYS]; an entity in view leaves at once, such as
   * one on another team.
   */
  NEVER,
}
