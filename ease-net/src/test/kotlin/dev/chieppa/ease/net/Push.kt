package dev.chieppa.ease.net

import com.github.quillraven.fleks.World
import dev.chieppa.ease.commands.Command
import dev.chieppa.ease.commands.CommandScope
import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.ecs.replication.NetworkId
import dev.chieppa.ease.ecs.replication.entityOf
import dev.chieppa.ease.ecs.replication.replication
import kotlin.math.abs
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Moves an entity its sender owns along x by at most [MAX_STEP] a command. */
@Serializable
@SerialName("test.Push")
internal class Push(val entity: NetworkId, val dx: Float) : Command {
  override val scope: CommandScope
    get() = CommandScope.OWNED_ENTITIES

  override fun targets(): List<NetworkId> = listOf(entity)

  override fun validate(world: World): String? =
    if (abs(dx) > MAX_STEP) "A push moves at most $MAX_STEP, not $dx" else null

  override fun apply(world: World): Command {
    val target = world.entityOf(entity)
    world.replication.write(target, LocalTransform) { x += dx }
    return Push(entity, -dx)
  }

  companion object {
    const val MAX_STEP = 1f
  }
}
