package dev.chieppa.ease.commands

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.Snapshot
import kotlinx.serialization.Serializable

/** One entity and everything it carries, as [WorldCodec.worldToJson] writes it. */
@Serializable internal class EntityState(val entity: Entity, val snapshot: Snapshot)
