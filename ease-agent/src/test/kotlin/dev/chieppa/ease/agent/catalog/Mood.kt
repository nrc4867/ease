package dev.chieppa.ease.agent.catalog

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** How a crate's owner feels, for the catalog tests. */
@Serializable
@SerialName("test.Mood")
internal enum class Mood {
  CALM,
  @SerialName("angry") ANGRY,
}
