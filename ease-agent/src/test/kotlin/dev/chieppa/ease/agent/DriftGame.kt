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
import dev.chieppa.ease.ecs.replication.replication
import dev.chieppa.ease.render.scene.Camera
import dev.chieppa.ease.render.scene.DirectionalLight
import dev.chieppa.ease.render.scene.Material
import dev.chieppa.ease.render.scene.Mesh
import dev.chieppa.ease.render.scene.MeshShape
import dev.chieppa.ease.runtime.GameModule
import dev.chieppa.ease.runtime.HostedWorld

/**
 * A small game: a glowing cube that drifts a tenth of a unit along x every tick and a still one,
 * then the camera that sees them and the sun that lights them.
 */
internal class DriftGame : GameModule {
  override fun registerTypes(registry: CommandRegistry) {
    registry.component(Glow::class, Glow.serializer())
  }

  override fun systems(systems: PhasedSystems) {
    systems.add(Phase.SIMULATION, DriftSystem())
  }

  override fun populate(world: HostedWorld) {
    world.execute(Spawn(cube(-2f, red = 1f, glow = true)))
    world.execute(Spawn(cube(2f, red = 0f, glow = false)))
    val camera = LocalTransform().also { it.z = CAMERA_DISTANCE }
    world.execute(Spawn(listOf<Component<out Any>>(camera, Camera.of())))
    world.execute(Spawn(listOf<Component<out Any>>(LocalTransform(), DirectionalLight.of())))
  }

  private fun cube(x: Float, red: Float, glow: Boolean): List<Component<out Any>> =
    listOfNotNull(
      LocalTransform().also {
        it.x = x
        it.scaleX = 2f
        it.scaleY = 2f
        it.scaleZ = 2f
      },
      Mesh.of(MeshShape.CUBE),
      Material.of(red = red, green = 1f, blue = 0f),
      if (glow) Glow().also { it.level = 1f } else null,
    )

  private class DriftSystem : IteratingSystem(family { all(LocalTransform, Glow) }) {
    private val replication = world.replication

    override fun onTickEntity(entity: Entity) {
      replication.write(entity, LocalTransform) { x += STEP }
    }
  }

  companion object {
    const val STEP = 0.1f
    const val CAMERA_DISTANCE = 8f
  }
}
