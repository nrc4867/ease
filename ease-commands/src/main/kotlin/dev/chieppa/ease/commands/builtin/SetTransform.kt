package dev.chieppa.ease.commands.builtin

import com.github.quillraven.fleks.World
import dev.chieppa.ease.annotations.Doc
import dev.chieppa.ease.commands.Command
import dev.chieppa.ease.commands.CommandRejected
import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.ecs.replication.NetworkId
import dev.chieppa.ease.ecs.replication.replication
import dev.chieppa.ease.math.Transform
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Doc(
  "Sets the LocalTransform of an entity; undone by setting the previous one back. Rejected " +
    "unless the world holds authority over it."
)
@Serializable
@SerialName("ease.SetTransform")
public class SetTransform(
  @Doc("The entity to move.") public val entity: NetworkId,
  @Doc("The transform to set: translation, rotation and scale.")
  @Serializable(with = TransformSerializer::class)
  public val transform: Transform,
) : Command {
  override fun apply(world: World): Command {
    val target = heldEntity(world, entity)
    val previous =
      with(world) {
        if (!target.has(LocalTransform)) {
          throw CommandRejected("Entity $entity has no LocalTransform to set")
        }
        target[LocalTransform].get()
      }
    world.replication.write(target, LocalTransform) { set(transform) }
    return SetTransform(entity, previous)
  }
}
