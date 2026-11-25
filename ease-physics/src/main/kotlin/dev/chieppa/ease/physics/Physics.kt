package dev.chieppa.ease.physics

import com.github.quillraven.fleks.World
import dev.chieppa.ease.commands.CommandRegistry
import dev.chieppa.ease.ecs.Phase
import dev.chieppa.ease.ecs.PhasedSystems
import dev.chieppa.ease.physics.body.Collider
import dev.chieppa.ease.physics.body.RigidBody
import dev.chieppa.ease.physics.character.CharacterController
import dev.chieppa.ease.physics.character.CharacterMotion
import dev.chieppa.ease.physics.character.CharacterMotionReplicated
import dev.chieppa.ease.physics.setup.PhysicsSettings
import dev.chieppa.ease.physics.volume.TriggerSystem
import dev.chieppa.ease.physics.volume.TriggerVolume
import dev.chieppa.ease.physics.volume.VolumeSensor
import dev.chieppa.ease.scene.types.SceneTypes

/** How a game adds physics to its worlds. */
public object Physics {
  /**
   * Adds a new [PhysicsWorld] built from [settings] to the world [systems] builds, with its systems
   * in the physics phase: bodies kept in step with colliders and stepped, characters made for
   * character controllers, then trigger volumes checked. Call it from `GameModule.systems`, so
   * every world, host and client, has the colliders its predicted characters walk against. Returns
   * the new physics world, which the world closes when it is disposed.
   */
  public fun addTo(
    systems: PhasedSystems,
    settings: PhysicsSettings = PhysicsSettings(),
  ): PhysicsWorld {
    val physics = PhysicsWorld(settings)
    systems.add(Phase.PHYSICS, PhysicsStepSystem(physics))
    systems.add(Phase.PHYSICS, CharacterBodiesSystem(physics))
    systems.add(Phase.PHYSICS, TriggerSystem(physics))
    return physics
  }

  /** Registers the physics component types scene files hold. */
  public fun registerSceneTypes(types: SceneTypes.Builder) {
    types.component(Collider, Collider.serializer())
    types.component(RigidBody, RigidBody.serializer())
    types.component(TriggerVolume, TriggerVolume.serializer())
    types.component(VolumeSensor, VolumeSensor.serializer())
    types.component(CharacterController, CharacterController.serializer())
    types.component(CharacterMotion, CharacterMotion.serializer())
  }

  /** Registers the physics component types that replicate. */
  public fun registerTypes(registry: CommandRegistry) {
    registry.replicated(CharacterMotionReplicated)
  }
}

/**
 * This world's physics, added by [Physics.addTo]. Finding it searches the world's systems, so a
 * system looks it up once, in `onInit` or on first use, rather than every tick.
 */
public val World.physics: PhysicsWorld
  get() = system<PhysicsStepSystem>().physics
