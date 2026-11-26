package dev.chieppa.ease.gltf

import dev.chieppa.ease.memory.MappedRegion
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Whole files: structure, transforms, cameras, lights, images and what is refused. */
class GltfFileTest {
  @Test
  fun `nodes, skins, cameras and animations are read as written`() {
    val skin = Samples.read("SimpleSkin.gltf").root
    val cameras = Samples.read("Cameras.gltf").root

    assertEquals(listOf(1, 2), skin.skins.single().joints)
    val animation = skin.animations.single()
    val channel = animation.channels.single()
    assertEquals("rotation", channel.target.path)
    assertEquals(0.7f, cameras.cameras[0].perspective?.yfov)
    assertEquals("orthographic", cameras.cameras[1].type)
  }

  @Test
  fun `a node's transform comes from its parts or is split from its matrix`() {
    val file =
      Samples.json(
        """
        {"asset": {"version": "2.0"},
         "nodes": [
           {"translation": [1, 2, 3]},
           {"matrix": [0, 2, 0, 0,  -2, 0, 0, 0,  0, 0, 2, 0,  4, 5, 6, 1]}]}
        """
      )

    val parts = NodeTransform.of(file.root.nodes[0], 0)
    val split = NodeTransform.of(file.root.nodes[1], 1)

    assertContentEquals(floatArrayOf(1f, 2f, 3f), parts.translation)
    assertContentEquals(floatArrayOf(0f, 0f, 0f, 1f), parts.rotation)
    assertContentEquals(floatArrayOf(4f, 5f, 6f), split.translation)
    assertContentEquals(floatArrayOf(2f, 2f, 2f), split.scale)
    val halfRoot = kotlin.math.sqrt(0.5f)
    assertEquals(halfRoot, split.rotation[2], 1e-6f)
    assertEquals(halfRoot, split.rotation[3], 1e-6f)
  }

  @Test
  fun `punctual lights are read from the extension and found by node`() {
    val file =
      Samples.json(
        """
        {"asset": {"version": "2.0"},
         "extensionsUsed": ["KHR_lights_punctual"],
         "extensions": {"KHR_lights_punctual": {"lights": [
           {"type": "directional", "color": [1, 0.5, 0.25], "intensity": 2, "name": "sun"}]}},
         "nodes": [{"extensions": {"KHR_lights_punctual": {"light": 0}}}, {}]}
        """
      )

    val light = checkNotNull(file.lightOf(0))
    assertEquals("directional", light.type)
    assertContentEquals(floatArrayOf(1f, 0.5f, 0.25f), light.color)
    assertEquals(2f, light.intensity)
    assertNull(file.lightOf(1))
  }

  @Test
  fun `buffers and images beside the file are opened by their decoded path`() {
    val opened = mutableListOf<String>()
    val files = SiblingFiles {
      opened += it
      MappedRegion.of(ByteArray(4) { index -> index.toByte() }, it)
    }
    val file =
      Samples.json(
        """
        {"asset": {"version": "2.0"},
         "buffers": [{"byteLength": 4, "uri": "my%20mesh.bin"}],
         "images": [{"uri": "skin.png"}]}
        """,
        files,
      )

    val image = file.image(0, files)

    assertEquals(listOf("my mesh.bin", "skin.png"), opened)
    assertEquals("image/png", image.mimeType)
    assertContentEquals(byteArrayOf(0, 1, 2, 3), image.bytes)
  }

  @Test
  fun `a required extension the reader lacks is refused by name`() {
    val failure =
      assertFailsWith<GltfException> {
        Samples.json(
          """
          {"asset": {"version": "2.0"}, "extensionsRequired": ["KHR_draco_mesh_compression"]}
          """
        )
      }

    assertEquals("extensionsRequired", failure.element)
  }

  @Test
  fun `required texture transforms are refused, since materials do not apply them`() {
    val failure =
      assertFailsWith<GltfException> {
        Samples.json(
          """
          {"asset": {"version": "2.0"}, "extensionsRequired": ["KHR_texture_transform"]}
          """
        )
      }

    assertTrue("KHR_texture_transform" in failure.message.orEmpty())
  }

  @Test
  fun `a missing buffer file names the buffer`() {
    val failure =
      assertFailsWith<GltfException> {
        Samples.json(
          """{"asset": {"version": "2.0"}, "buffers": [{"byteLength": 4, "uri": "a.bin"}]}"""
        )
      }

    assertEquals("buffers[0]", failure.element)
  }

  @Test
  fun `glTF 1 is refused`() {
    assertFailsWith<GltfException> { Samples.json("""{"asset": {"version": "1.0"}}""") }
  }
}
