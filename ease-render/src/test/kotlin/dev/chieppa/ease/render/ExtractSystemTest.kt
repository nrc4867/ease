package dev.chieppa.ease.render

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.ecs.Phase
import dev.chieppa.ease.ecs.phasedWorld
import kotlin.test.Test
import kotlin.test.assertEquals

/** Worlds ticked by hand, then what the extract wrote read back from the snapshot. */
class ExtractSystemTest {
  private val snapshot = RenderSnapshot(initialCapacity = 2)
  private val world: World = phasedWorld { add(Phase.EXTRACT, ExtractSystem(snapshot)) }

  private fun quadAt(x: Float, y: Float): Entity = world.entity {
    it +=
      LocalTransform().apply {
        this.x = x
        this.y = y
      }
    it += Quad.of(size = 2f, red = 0.25f, green = 0.5f, blue = 0.75f)
  }

  private fun moveTo(entity: Entity, x: Float, y: Float) =
    with(world) {
      entity[LocalTransform].x = x
      entity[LocalTransform].y = y
    }

  @Test
  fun `an entity seen for the first time starts where it is`() {
    val entity = quadAt(3f, 4f)

    world.update(DELTA)

    assertEquals(1, snapshot.count)
    assertEquals(entity.id, snapshot.entityAt(0))
    assertEquals(3f, snapshot.previousX[entity.id])
    assertEquals(4f, snapshot.previousY[entity.id])
    assertEquals(3f, snapshot.x(entity.id, alpha = 0.5f))
  }

  @Test
  fun `each tick moves the current position to previous`() {
    val entity = quadAt(0f, 0f)
    world.update(DELTA)

    moveTo(entity, 2f, -4f)
    world.update(DELTA)

    assertEquals(0f, snapshot.previousX[entity.id])
    assertEquals(2f, snapshot.currentX[entity.id])
    assertEquals(-4f, snapshot.currentY[entity.id])
    assertEquals(1f, snapshot.x(entity.id, alpha = 0.5f))
    assertEquals(-2f, snapshot.y(entity.id, alpha = 0.5f))
  }

  @Test
  fun `size and color come from the quad`() {
    val entity = quadAt(0f, 0f)

    world.update(DELTA)

    assertEquals(2f, snapshot.size[entity.id])
    assertEquals(0.25f, snapshot.red[entity.id])
    assertEquals(0.5f, snapshot.green[entity.id])
    assertEquals(0.75f, snapshot.blue[entity.id])
  }

  @Test
  fun `entities without a quad are not drawn`() {
    world.entity { it += LocalTransform() }
    quadAt(1f, 1f)

    world.update(DELTA)

    assertEquals(1, snapshot.count)
  }

  @Test
  fun `the snapshot grows past its starting capacity`() {
    val entities = List(MANY) { quadAt(it.toFloat(), 0f) }

    world.update(DELTA)

    assertEquals(MANY, snapshot.count)
    entities.forEach { assertEquals(it.id.toFloat(), snapshot.currentX[it.id]) }
  }

  @Test
  fun `an entity that skipped a tick starts again where it is`() {
    val entity = quadAt(0f, 0f)
    world.update(DELTA)
    with(world) { entity.configure { it -= Quad } }
    world.update(DELTA)

    moveTo(entity, 5f, 0f)
    with(world) { entity.configure { it += Quad.of(1f, 1f, 1f, 1f) } }
    world.update(DELTA)

    assertEquals(5f, snapshot.previousX[entity.id])
  }

  private companion object {
    const val DELTA = 1f / 60f
    const val MANY = 9
  }
}
