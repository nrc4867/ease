package dev.chieppa.ease.physics

import dev.chieppa.ease.physics.setup.CollisionLayers
import dev.chieppa.ease.physics.setup.SurfaceType
import dev.chieppa.ease.physics.setup.SurfaceTypes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Collision layers and surface types resolve names, and refuse what is named twice. */
class CollisionLayersTest {
  @Test
  fun `every pair collides unless ignored, and unknown names are the first layer`() {
    val layers = CollisionLayers.build {
      layer("A")
      layer("B")
      layer("C")
      ignore("C", "A")
    }

    assertTrue(layers.collide(0, 1))
    assertTrue(layers.collide(2, 2))
    assertFalse(layers.collide(0, 2))
    assertFalse(layers.collide(2, 0))
    assertEquals(1, layers.resolve("B"))
    assertEquals(0, layers.resolve("missing"))
    assertEquals(-1, layers.indexOf("missing"))
    assertFailsWith<IllegalArgumentException> {
      CollisionLayers.build {
        layer("A")
        layer("A")
      }
    }
    assertFailsWith<IllegalArgumentException> { CollisionLayers.build { ignore("A", "B") } }
  }

  @Test
  fun `a game naming no layers has the default one`() {
    assertEquals(CollisionLayers.DEFAULT, CollisionLayers.build().name(0))
  }

  @Test
  fun `surface types follow the default, which unknown names resolve to`() {
    val surfaces = SurfaceTypes.of(SurfaceType("mud"))

    assertEquals(SurfaceTypes.DEFAULT, surfaces[0].name)
    assertEquals(1, surfaces.resolve("mud"))
    assertEquals(0, surfaces.resolve("lava"))
    assertFailsWith<IllegalArgumentException> {
      SurfaceTypes.of(SurfaceType("mud"), SurfaceType("mud"))
    }
  }
}
