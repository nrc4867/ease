package dev.chieppa.ease.sample

import com.github.quillraven.fleks.Entity
import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.render.scene.Material
import dev.chieppa.ease.runtime.GameModules
import dev.chieppa.ease.runtime.HostedWorld
import dev.chieppa.ease.runtime.loop.FixedStep
import java.nio.file.Path

/**
 * The sample world as a headless host makes it: the game found on the classpath, over a copy of its
 * content in [root], no window.
 */
internal class SampleWorld(root: Path) {
  val fixedStep: FixedStep = FixedStep.perSecond()
  val module = GameModules.load()
  val content = SampleContent(root)
  val hosted = HostedWorld(module, fixedStep, assets = content.assets)
  val stepSeconds: Float = stepSeconds(fixedStep)

  /** The world's entities in id order: the playfield scene's nodes. */
  val nodes: List<Entity> = with(hosted.world) { asEntityBag().map { it }.sortedBy { it.id } }

  /** The scene's cubes, the entities with a velocity, in id order. */
  val entities: List<Entity> = with(hosted.world) { nodes.filter { it.has(Velocity) } }

  fun step(ticks: Int) = repeat(ticks) { hosted.step() }

  fun x(entity: Entity): Float = with(hosted.world) { entity[LocalTransform].x }

  fun y(entity: Entity): Float = with(hosted.world) { entity[LocalTransform].y }

  /** [entity]'s velocity along x and y. */
  fun velocity(entity: Entity): Pair<Float, Float> =
    with(hosted.world) { entity[Velocity].x to entity[Velocity].y }

  fun z(entity: Entity): Float = with(hosted.world) { entity[LocalTransform].z }

  /** [entity]'s material base color, red, green and blue. */
  fun color(entity: Entity): FloatArray =
    with(hosted.world) {
      val material = entity[Material]
      floatArrayOf(material.red, material.green, material.blue)
    }
}
