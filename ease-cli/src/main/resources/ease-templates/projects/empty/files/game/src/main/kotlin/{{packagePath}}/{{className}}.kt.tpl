package {{package}}

import com.github.quillraven.fleks.Component
import dev.chieppa.ease.commands.CommandRegistry
import dev.chieppa.ease.commands.builtin.Spawn
import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.ecs.PhasedSystems
import dev.chieppa.ease.render.scene.Camera
import dev.chieppa.ease.render.scene.DirectionalLight
import dev.chieppa.ease.render.scene.Mesh
import dev.chieppa.ease.render.scene.MeshShape
import dev.chieppa.ease.runtime.GameModule
import dev.chieppa.ease.runtime.HostedWorld
import dev.chieppa.ease.scene.types.SceneTypes

/** {{projectName}}: one white cube at the origin, seen by a camera and lit by a sun. */
public class {{className}} : GameModule {
  override fun registerTypes(registry: CommandRegistry) {
    // ease:types
  }

  override fun registerSceneTypes(types: SceneTypes.Builder) {
    // ease:scene-types
  }

  override fun hostSystems(systems: PhasedSystems) {
    // ease:systems
  }

  override fun populate(world: HostedWorld) {
    val cube: List<Component<out Any>> = listOf(LocalTransform(), Mesh.of(MeshShape.CUBE))
    world.execute(Spawn(cube))
    val eye = LocalTransform().also { it.z = CAMERA_DISTANCE }
    world.execute(Spawn(listOf<Component<out Any>>(eye, Camera.of())))
    world.execute(Spawn(listOf<Component<out Any>>(LocalTransform(), DirectionalLight.of())))
  }

  private companion object {
    const val CAMERA_DISTANCE = 4f
  }
}
