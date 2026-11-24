package dev.chieppa.ease.agent.catalog

import dev.chieppa.ease.annotations.Doc
import dev.chieppa.ease.annotations.Range
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/** A class using every field shape the schemas describe, for the catalog tests. */
@Doc("A crate.")
@Serializable
@SerialName("test.Crate")
internal class Crate(
  val id: CrateId,
  val label: String?,
  val tags: List<String>,
  val weights: Map<String, Int>,
  val mood: Mood,
  val weather: Weather,
) {
  @SerialName("lid") var hasLid: Boolean = true

  @Doc("How tall it is.") @Range(0.0, 9.5) var height: Double? = null

  @Serializable var again: Mood = Mood.CALM

  @Transient val scratch: Int = 0
}
