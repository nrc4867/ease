package {{package}}

import com.github.quillraven.fleks.Component
import dev.chieppa.ease.commands.CommandRegistry
import dev.chieppa.ease.commands.builtin.Spawn
import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.ecs.PhasedSystems
import dev.chieppa.ease.render.Quad
import dev.chieppa.ease.runtime.GameModule
import dev.chieppa.ease.runtime.HostedWorld

/** {{projectName}}: one white square at the origin. */
public class {{className}} : GameModule {
  override fun registerTypes(registry: CommandRegistry) {
    // ease:types
  }

  override fun systems(systems: PhasedSystems) {
    // ease:systems
  }

  override fun populate(world: HostedWorld) {
    val square: List<Component<out Any>> =
      listOf(LocalTransform(), Quad.of(SQUARE_SIZE, 1f, 1f, 1f))
    world.execute(Spawn(square))
  }

  private companion object {
    const val SQUARE_SIZE = 1f
  }
}
