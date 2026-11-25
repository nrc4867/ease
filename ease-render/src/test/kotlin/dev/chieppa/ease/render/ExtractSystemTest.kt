package dev.chieppa.ease.render

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.ecs.Phase
import dev.chieppa.ease.ecs.phasedWorld
import dev.chieppa.ease.gpu.lit.MaterialFloats
import dev.chieppa.ease.gpu.mesh.MeshData
import dev.chieppa.ease.render.scene.Camera
import dev.chieppa.ease.render.scene.CustomMesh
import dev.chieppa.ease.render.scene.DirectionalLight
import dev.chieppa.ease.render.scene.Material
import dev.chieppa.ease.render.scene.Mesh
import dev.chieppa.ease.render.scene.MeshShape
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** Worlds ticked by hand, then what the extract wrote read back from the snapshot. */
class ExtractSystemTest {
  private val snapshot = RenderSnapshot(initialCapacity = 2)
  private val world: World = phasedWorld { add(Phase.EXTRACT, ExtractSystem(snapshot)) }
  private val blended = FloatArray(TRANSFORM_FLOATS)

  private fun cubeAt(x: Float, y: Float, z: Float = 0f): Entity = world.entity {
    it += transformAt(x, y, z)
    it += Mesh.of(MeshShape.CUBE)
    it += Material.of(0.25f, 0.5f, 0.75f, roughness = 0.3f)
  }

  private fun transformAt(x: Float, y: Float, z: Float) =
    LocalTransform().apply {
      this.x = x
      this.y = y
      this.z = z
    }

  private fun moveTo(entity: Entity, x: Float, y: Float, z: Float) =
    with(world) {
      entity[LocalTransform].x = x
      entity[LocalTransform].y = y
      entity[LocalTransform].z = z
    }

  private fun blend(entity: Entity, alpha: Float): List<Float> {
    snapshot.transformInto(entity.id, alpha, blended, 0)
    return blended.toList()
  }

  @Test
  fun `an entity seen for the first time starts where it is`() {
    val entity = cubeAt(3f, 4f, 5f)

    world.update(DELTA)

    assertEquals(1, snapshot.count)
    assertEquals(entity.id, snapshot.entityAt(0))
    assertEquals(listOf(3f, 4f, 5f), blend(entity, 0f).take(3))
  }

  @Test
  fun `each tick moves the current transform to previous, and frames blend between them`() {
    val entity = cubeAt(0f, 0f)
    world.update(DELTA)

    moveTo(entity, 2f, -4f, 6f)
    world.update(DELTA)

    assertEquals(listOf(0f, 0f, 0f), blend(entity, 0f).take(3))
    assertEquals(listOf(1f, -2f, 3f), blend(entity, 0.5f).take(3))
    assertEquals(listOf(0f, 0f, 0f, 1f), blend(entity, 0.5f).subList(ROTATION_AT, SCALE_AT))
    assertEquals(listOf(1f, 1f, 1f), blend(entity, 0.5f).drop(SCALE_AT))
  }

  @Test
  fun `the shape and the material come from the entity's components`() {
    val entity = cubeAt(0f, 0f)

    world.update(DELTA)

    assertEquals(MeshShape.CUBE, snapshot.shape(entity.id))
    assertEquals(0.25f, snapshot.material(entity.id, MaterialFloats.BASE_COLOR))
    assertEquals(0.75f, snapshot.material(entity.id, MaterialFloats.BASE_COLOR + 2))
    assertEquals(0.3f, snapshot.material(entity.id, MaterialFloats.ROUGHNESS))
  }

  @Test
  fun `a runtime mesh is drawn in place of a shape`() {
    MeshData().use { data ->
      val entity = world.entity {
        it += LocalTransform()
        it += Mesh.of(MeshShape.SPHERE)
        it += CustomMesh(data)
      }

      world.update(DELTA)

      assertSame(data, snapshot.customMesh(entity.id))
      assertNull(snapshot.shape(entity.id))
    }
  }

  @Test
  fun `the camera and the light of the lowest entity ids are the ones found`() {
    world.entity { it += LocalTransform() }
    val camera = world.entity {
      it += transformAt(0f, 2f, 8f)
      it += Camera.of(fieldOfView = 45f)
    }
    world.entity {
      it += LocalTransform()
      it += Camera.of(fieldOfView = 90f)
    }
    val light = world.entity {
      it += LocalTransform()
      it += DirectionalLight.of(intensity = 2f)
    }

    world.update(DELTA)

    assertEquals(camera.id, snapshot.camera.entity)
    assertEquals(45f, snapshot.camera.fieldOfView)
    assertEquals(light.id, snapshot.light.entity)
    assertEquals(2f, snapshot.light.intensity)
    assertEquals(0, snapshot.count)
    assertEquals(listOf(0f, 2f, 8f), blend(camera, 1f).take(3))
  }

  @Test
  fun `a world without a camera or light has neither`() {
    cubeAt(0f, 0f)

    world.update(DELTA)

    assertFalse(snapshot.camera.present)
    assertFalse(snapshot.light.present)
  }

  @Test
  fun `the snapshot grows past its starting capacity`() {
    val entities = List(MANY) { cubeAt(it.toFloat(), 0f) }

    world.update(DELTA)

    assertEquals(MANY, snapshot.count)
    entities.forEach { assertEquals(it.id.toFloat(), blend(it, 1f)[0]) }
  }

  @Test
  fun `an entity that skipped a tick starts again where it is`() {
    val entity = cubeAt(0f, 0f)
    world.update(DELTA)
    with(world) { entity.configure { it -= Mesh } }
    world.update(DELTA)

    moveTo(entity, 5f, 0f, 0f)
    with(world) { entity.configure { it += Mesh.of(MeshShape.PLANE) } }
    world.update(DELTA)

    assertEquals(5f, blend(entity, 0f)[0])
    assertTrue(snapshot.count == 1)
  }

  private companion object {
    const val DELTA = 1f / 60f
    const val MANY = 9
  }
}
