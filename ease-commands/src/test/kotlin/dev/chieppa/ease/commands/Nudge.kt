package dev.chieppa.ease.commands

import com.github.quillraven.fleks.World
import dev.chieppa.ease.ecs.replication.NetworkId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A command a game would register itself; this one only needs to serialize. */
@Serializable
@SerialName("test.Nudge")
internal class Nudge(val entity: NetworkId, val distance: Float) : Command {
  override fun apply(world: World): Command = Nudge(entity, -distance)
}
