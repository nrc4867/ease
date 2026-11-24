package dev.chieppa.ease.sample

import com.github.quillraven.fleks.Entity
import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.runtime.GameModules
import dev.chieppa.ease.runtime.HostedWorld
import dev.chieppa.ease.runtime.loop.FixedStep

/** The sample world as a headless host makes it: the game found on the classpath, no window. */
internal class SampleWorld {
  val fixedStep: FixedStep = FixedStep.perSecond()
  val module = GameModules.load()
  val hosted = HostedWorld(module, fixedStep)
  val stepSeconds: Float = fixedStep.stepNanos / NANOS_PER_SECOND

  /** The world's entities in id order, which is the order the game spawned its squares in. */
  val entities: List<Entity> = with(hosted.world) { asEntityBag().map { it }.sortedBy { it.id } }

  fun step(ticks: Int) = repeat(ticks) { hosted.step() }

  fun x(entity: Entity): Float = with(hosted.world) { entity[LocalTransform].x }

  fun y(entity: Entity): Float = with(hosted.world) { entity[LocalTransform].y }

  private companion object {
    const val NANOS_PER_SECOND = 1_000_000_000f
  }
}
