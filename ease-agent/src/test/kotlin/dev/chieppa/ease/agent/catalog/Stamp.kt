package dev.chieppa.ease.agent.catalog

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A class whose serializer, not its properties, decides its shape; for the catalog tests. */
@Serializable(with = StampSerializer::class)
@SerialName("test.Stamp")
internal class Stamp(val tick: Int)
