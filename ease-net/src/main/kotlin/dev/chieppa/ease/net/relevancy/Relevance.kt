package dev.chieppa.ease.net.relevancy

/** What one [RelevancyFilter] says about an entity for one viewer. */
public enum class Relevance {
  /** Relevant whatever the other filters say, such as an entity marked always relevant. */
  ALWAYS,

  /** No objection: relevant unless another filter says [NEVER]. */
  PASS,

  /** Not relevant, unless another filter says [ALWAYS]. */
  NEVER,
}
