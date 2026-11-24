package dev.chieppa.ease.agent

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import dev.chieppa.ease.commands.CommandRegistry
import dev.chieppa.ease.commands.builtin.Spawn
import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.ecs.Phase
import dev.chieppa.ease.ecs.PhasedSystems
import dev.chieppa.ease.render.Camera2D
import dev.chieppa.ease.render.Quad
import dev.chieppa.ease.runtime.GameModule
import dev.chieppa.ease.runtime.HostedWorld

/**
 * A small game: a glowing quad that drifts a tenth of a unit along x every tick, and a still one.
 */
internal class DriftGame : GameModule {
  override fun registerTypes(registry: CommandRegistry) {
    registry.component(Glow::class, Glow.serializer())
  }

  override fun systems(systems: PhasedSystems) {
    systems.add(Phase.SIMULATION, DriftSystem())
  }

  override fun populate(world: HostedWorld) {
    world.execute(Spawn(quad(-2f, red = 1f, glow = true)))
    world.execute(Spawn(quad(2f, red = 0f, glow = false)))
  }

  override fun camera(): Camera2D = Camera2D(visibleHeight = VISIBLE_HEIGHT)

  private fun quad(x: Float, red: Float, glow: Boolean): List<Component<out Any>> =
    listOfNotNull(
      LocalTransform().also { it.x = x },
      Quad.of(size = 2f, red = red, green = 1f, blue = 0f),
      if (glow) Glow().also { it.level = 1f } else null,
    )

  private class DriftSystem : IteratingSystem(family { all(LocalTransform, Glow) }) {
    override fun onTickEntity(entity: Entity) {
      entity[LocalTransform].x += STEP
    }
  }

  companion object {
    const val STEP = 0.1f
    const val VISIBLE_HEIGHT = 8f
  }
}
