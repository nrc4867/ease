package dev.chieppa.ease.agent.catalog

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** What a [Stamp] is written as. */
@Serializable @SerialName("test.StampData") internal class StampData(val tick: Int)
