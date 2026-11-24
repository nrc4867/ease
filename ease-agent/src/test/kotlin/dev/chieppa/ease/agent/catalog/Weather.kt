package dev.chieppa.ease.agent.catalog

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A sealed hierarchy for the catalog tests. */
@Serializable
@SerialName("test.Weather")
internal sealed class Weather {
  @Serializable @SerialName("test.Rain") class Rain(val millimeters: Float) : Weather()

  @Serializable @SerialName("test.Sun") class Sun(val hours: Int = 8) : Weather()
}
