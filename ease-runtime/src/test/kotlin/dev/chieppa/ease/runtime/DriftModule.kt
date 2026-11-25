package dev.chieppa.ease.runtime

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import dev.chieppa.ease.commands.builtin.Spawn
import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.ecs.Phase
import dev.chieppa.ease.ecs.PhasedSystems
import dev.chieppa.ease.ecs.replication.replication
import dev.chieppa.ease.render.scene.Mesh
import dev.chieppa.ease.render.scene.MeshShape

/** A game whose one cube drifts one unit along x every tick, moved by a host system. */
internal class DriftModule : GameModule {
  override fun hostSystems(systems: PhasedSystems) {
    systems.add(Phase.SIMULATION, DriftSystem())
  }

  override fun populate(world: HostedWorld) {
    world.execute(Spawn(listOf<Component<out Any>>(LocalTransform(), Mesh.of(MeshShape.CUBE))))
  }

  private class DriftSystem : IteratingSystem(family { all(LocalTransform) }) {
    private val replication = world.replication

    override fun onTickEntity(entity: Entity) {
      replication.write(entity, LocalTransform) { x += 1f }
    }
  }
}
