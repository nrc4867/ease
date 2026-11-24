package dev.chieppa.ease.ecs

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World
import com.github.quillraven.fleks.World.Companion.family
import java.util.concurrent.TimeUnit
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State

class BenchPosition(var x: Float = 0f, var y: Float = 0f) : Component<BenchPosition> {
  override fun type() = BenchPosition

  companion object : ComponentType<BenchPosition>()
}

class BenchVelocity(var x: Float = 0f, var y: Float = 0f) : Component<BenchVelocity> {
  override fun type() = BenchVelocity

  companion object : ComponentType<BenchVelocity>()
}

class BenchMovementSystem : IteratingSystem(family { all(BenchPosition, BenchVelocity) }) {
  override fun onTickEntity(entity: Entity) {
    val position = entity[BenchPosition]
    val velocity = entity[BenchVelocity]
    position.x += velocity.x
    position.y += velocity.y
  }
}

/** One tick of a movement system over a family of 10,000 entities. */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
open class MovementBenchmark {
  private lateinit var world: World

  @Setup
  fun build() {
    world = phasedWorld(ENTITIES) { add(Phase.SIMULATION, BenchMovementSystem()) }
    repeat(ENTITIES) { _ ->
      world.entity {
        it += BenchPosition()
        it += BenchVelocity(1f, 1f)
      }
    }
  }

  @Benchmark fun tick() = world.update(DELTA)

  private companion object {
    const val ENTITIES = 10_000
    const val DELTA = 1f / 60f
  }
}
