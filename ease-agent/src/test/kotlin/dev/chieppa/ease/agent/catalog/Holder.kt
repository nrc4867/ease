package dev.chieppa.ease.agent.catalog

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Holds a member of a sealed hierarchy directly, and another through its base. */
@Serializable
@SerialName("test.Holder")
internal class Holder(val rain: Weather.Rain, val weather: Weather)
