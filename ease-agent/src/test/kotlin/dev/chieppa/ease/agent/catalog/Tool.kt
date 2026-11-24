package dev.chieppa.ease.agent.catalog

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A sealed class with a property of its own, for the catalog tests. */
@Serializable
@SerialName("test.Tool")
internal sealed class Tool(val weight: Int) {
  @Serializable @SerialName("test.Hammer") class Hammer(val name: String) : Tool(2)
}
