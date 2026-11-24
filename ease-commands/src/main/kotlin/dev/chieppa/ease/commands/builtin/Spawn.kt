package dev.chieppa.ease.commands.builtin

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.Snapshot
import com.github.quillraven.fleks.World
import dev.chieppa.ease.commands.Command
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Creates an entity carrying what [snapshot] holds; undone by destroying it. */
@Serializable
@SerialName("ease.Spawn")
public class Spawn(public val snapshot: Snapshot) : Command {
  /** Spawns an entity carrying [components] and no tags. */
  public constructor(components: List<Component<out Any>>) : this(Snapshot(components, emptyList()))

  override fun apply(world: World): Command {
    val entity = world.entity { it += snapshot.components }
    return Destroy(entity)
  }
}
