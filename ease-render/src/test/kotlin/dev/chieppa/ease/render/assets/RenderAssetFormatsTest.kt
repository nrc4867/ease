package dev.chieppa.ease.render.assets

import dev.chieppa.ease.assets.Guid
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

/** Cooked meshes and material assets read back what was written. */
class RenderAssetFormatsTest {
  private val guid = Guid(0, 1)

  @Test
  fun `a cooked mesh loads its vertices, indices, bounds and skin streams`() {
    val streams =
      MeshStreams(
        positions = floatArrayOf(0f, 0f, 0f, 1f, 0f, 0f, 0f, 2f, -1f),
        normals = floatArrayOf(0f, 0f, 1f, 0f, 0f, 1f, 0f, 0f, 1f),
        uv0 = FloatArray(6),
        tangents = floatArrayOf(1f, 0f, 0f, 1f, 1f, 0f, 0f, 1f, 1f, 0f, 0f, -1f),
        indices = intArrayOf(0, 1, 2),
        joints = intArrayOf(0, 1, 0, 0, 1, 0, 0, 0, 2, 0, 0, 0),
        weights = FloatArray(12) { if (it % 4 == 0) 1f else 0f },
      )

    MeshLoader.load(guid, MeshFormat.write(streams)).use { loaded ->
      assertEquals(3, loaded.mesh.vertexCount)
      assertEquals(3, loaded.mesh.indexCount)
      assertContentEquals(floatArrayOf(0f, 0f, -1f), loaded.boundsMin)
      assertContentEquals(floatArrayOf(1f, 2f, 0f), loaded.boundsMax)
      assertContentEquals(streams.joints, loaded.joints)
      assertEquals(null, loaded.colors)
    }
  }

  @Test
  fun `a material asset is canonical text that reads back`() {
    val material =
      MaterialAsset(
        baseColor = listOf(0.5f, 0.25f, 1f, 1f),
        roughness = 0.4f,
        alphaMode = AlphaMode.MASK,
        baseColorTexture = TextureSlot(guid.named("bark.ktx2")),
      )

    val bytes = MaterialFormat.write(material)
    val read = MaterialFormat.read(bytes)

    assertContentEquals(bytes, MaterialFormat.write(read))
    assertEquals(guid.named("bark.ktx2"), read.baseColorTexture?.texture)
    assertEquals(AlphaMode.MASK, read.alphaMode)
  }
}
