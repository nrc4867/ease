package dev.chieppa.ease.sample

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import dev.chieppa.ease.annotations.Doc
import dev.chieppa.ease.annotations.Range
import dev.chieppa.ease.commands.Command
import dev.chieppa.ease.commands.CommandRejected
import dev.chieppa.ease.commands.CommandScope
import dev.chieppa.ease.ecs.replication.NetworkId
import dev.chieppa.ease.ecs.replication.entityOf
import dev.chieppa.ease.ecs.replication.replication
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Doc(
  "Sets how fast a courier moves at full input; undone by setting the previous pace back. A " +
    "peer may set only the pace of a courier it owns."
)
@Serializable
@SerialName("sample.SetPace")
public class SetPace(
  @Doc("The courier.") public val entity: NetworkId,
  @Doc("The new pace, in world units a second.")
  @Range(0.0, Courier.MAX_PACE)
  public val pace: Float,
) : Command {
  override val scope: CommandScope
    get() = CommandScope.OWNED_ENTITIES

  override fun targets(): List<NetworkId> = listOf(entity)

  override fun validate(world: World): String? {
    val target = world.entityOf(entity)
    return when {
      target == Entity.NONE || !with(world) { target.has(Courier) } ->
        "Entity $entity is not a courier"
      !world.replication.hasAuthority(target) -> "Entity $entity is held by another peer"
      !(pace >= 0f && pace <= Courier.MAX_PACE) ->
        "A pace is from 0 to ${Courier.MAX_PACE} units a second"
      else -> null
    }
  }

  override fun apply(world: World): Command {
    validate(world)?.let { throw CommandRejected(it) }
    val target = world.entityOf(entity)
    val previous = SetPace(entity, with(world) { target[Courier].pace })
    world.replication.write(target, Courier) { pace = this@SetPace.pace }
    return previous
  }
}
