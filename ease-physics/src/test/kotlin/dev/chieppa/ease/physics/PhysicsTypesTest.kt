package dev.chieppa.ease.physics

import dev.chieppa.ease.physics.body.Collider
import dev.chieppa.ease.physics.body.ShapeKind
import dev.chieppa.ease.physics.volume.TriggerVolume
import dev.chieppa.ease.scene.types.SceneTypes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlinx.serialization.json.Json

/** The physics components are scene types, read and written by their serial names. */
class PhysicsTypesTest {
  @Test
  fun `every physics component is a scene type`() {
    val types = SceneTypes.build { Physics.registerSceneTypes(this) }

    for (name in
      listOf(
        "ease.Collider",
        "ease.RigidBody",
        "ease.TriggerVolume",
        "ease.VolumeSensor",
        "ease.CharacterController",
        "ease.CharacterMotion",
      )) {
      assertNotNull(types[name], name)
    }
  }

  @Test
  fun `a collider and a trigger round-trip through their serializers`() {
    val collider =
      Collider().apply {
        shape = ShapeKind.CAPSULE
        radius = 0.4f
        layer = "Player"
        surface = "stone"
      }
    val read =
      Json.decodeFromString(
        Collider.serializer(),
        Json.encodeToString(Collider.serializer(), collider),
      )
    assertEquals(
      listOf(ShapeKind.CAPSULE, 0.4f, "Player", "stone"),
      listOf(read.shape, read.radius, read.layer, read.surface),
    )

    val trigger =
      TriggerVolume().apply {
        once = true
        layers = listOf("Player")
      }
    val back =
      Json.decodeFromString(
        TriggerVolume.serializer(),
        Json.encodeToString(TriggerVolume.serializer(), trigger),
      )
    assertEquals(true, back.once)
    assertEquals(listOf("Player"), back.layers)
  }
}
