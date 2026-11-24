package dev.chieppa.ease.commands.builtin

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import dev.chieppa.ease.commands.Command
import dev.chieppa.ease.commands.CommandRejected
import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.math.Transform
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Sets [entity]'s [LocalTransform] to [transform]; undone by setting the previous one back. */
@Serializable
@SerialName("ease.SetTransform")
public class SetTransform(
  public val entity: Entity,
  @Serializable(with = TransformSerializer::class) public val transform: Transform,
) : Command {
  override fun apply(world: World): Command =
    with(world) {
      if (entity !in world || !entity.has(LocalTransform)) {
        throw CommandRejected("Entity ${entity.id} has no LocalTransform to set")
      }
      val component = entity[LocalTransform]
      val previous = component.get()
      component.set(transform)
      SetTransform(entity, previous)
    }
}
