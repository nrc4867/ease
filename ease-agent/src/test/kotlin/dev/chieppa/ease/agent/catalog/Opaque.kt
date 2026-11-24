package dev.chieppa.ease.agent.catalog

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A class with a custom serializer no generator can see through; for the catalog tests. */
@Serializable(with = OpaqueSerializer::class)
@SerialName("test.Opaque")
internal class Opaque(val code: Int)
