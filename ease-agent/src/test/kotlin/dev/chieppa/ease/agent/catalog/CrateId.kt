package dev.chieppa.ease.agent.catalog

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A value class for the catalog tests. */
@Serializable @SerialName("test.CrateId") @JvmInline internal value class CrateId(val value: Int)
