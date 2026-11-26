package dev.chieppa.ease.gltf

import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** Accessors read from Khronos samples and small hand-written files. */
class AccessorsTest {
  @Test
  fun `a sparse accessor replaces the elements it names`() {
    val positions = Samples.read("SimpleSparseAccessor.gltf").accessors.floats(1)

    assertEquals(14, positions.count)
    assertEquals(3, positions.components)
    assertEquals(1f, positions[8, 0])
    assertEquals(2f, positions[8, 1])
    assertEquals(3f, positions[10, 1])
    assertEquals(4f, positions[12, 1])
    assertEquals(1f, positions[9, 1])
  }

  @Test
  fun `interleaved joints and weights read through the view's stride`() {
    val file = Samples.read("SimpleSkin.gltf")
    val joints = file.accessors.ints(2)
    val weights = file.accessors.floats(3)

    assertEquals(1, joints[2, 1])
    assertEquals(0, joints[1, 1])
    assertEquals(0.75f, weights[2, 0])
    assertEquals(0.25f, weights[2, 1])
  }

  @Test
  fun `a matrix accessor reads column by column`() {
    val inverseBinds = Samples.read("SimpleSkin.gltf").accessors.floats(4)

    assertEquals(16, inverseBinds.components)
    assertEquals(-1f, inverseBinds[1, 13])
    assertEquals(1f, inverseBinds[1, 15])
  }

  @Test
  fun `a GLB's binary chunk holds its buffer`() {
    val file = Samples.read("BoxVertexColors.glb")
    val indices = file.accessors.ints(0)

    assertEquals(36, indices.count)
    assertEquals(24, file.accessors.floats(1).count)
  }

  @Test
  fun `normalized integers map to 0 to 1 and -1 to 1`() {
    val bytes = byteArrayOf(0, 127, -1, -128, -127, 0, 0, 0)
    val file =
      Samples.json(
        """
        {"asset": {"version": "2.0"},
         "buffers": [{"byteLength": 8, "uri": "${dataUri(bytes)}"}],
         "bufferViews": [{"buffer": 0, "byteLength": 8}],
         "accessors": [
           {"bufferView": 0, "componentType": 5121, "count": 3, "type": "SCALAR", "normalized": true},
           {"bufferView": 0, "byteOffset": 2, "componentType": 5120, "count": 3, "type": "SCALAR",
            "normalized": true}]}
        """
      )

    assertContentEquals(floatArrayOf(0f, 127f / 255f, 1f), file.accessors.floats(0).values)
    assertContentEquals(floatArrayOf(-1f / 127f, -1f, -1f), file.accessors.floats(1).values)
  }

  @Test
  fun `an accessor running past its view is an error naming it and its user`() {
    val file =
      Samples.json(
        """
        {"asset": {"version": "2.0"},
         "buffers": [{"byteLength": 8, "uri": "${dataUri(ByteArray(8))}"}],
         "bufferViews": [{"buffer": 0, "byteLength": 8}],
         "accessors": [{"bufferView": 0, "componentType": 5126, "count": 3, "type": "VEC3"}]}
        """
      )

    val failure =
      assertFailsWith<GltfException> {
        file.accessors.floats(0, "meshes[0].primitives[0].attributes.POSITION")
      }
    assertEquals("meshes[0].primitives[0].attributes.POSITION: accessors[0]", failure.element)
  }

  @Test
  fun `an accessor with no buffer view reads as zeros`() {
    val file =
      Samples.json(
        """
        {"asset": {"version": "2.0"},
         "accessors": [{"componentType": 5126, "count": 2, "type": "VEC2"}]}
        """
      )

    assertContentEquals(FloatArray(4), file.accessors.floats(0).values)
  }

  @Test
  fun `a float accessor is not read as integers`() {
    val failure =
      assertFailsWith<GltfException> { Samples.read("SimpleSkin.gltf").accessors.ints(3) }

    assertEquals("accessors[3]", failure.element)
  }

  private fun dataUri(bytes: ByteArray): String =
    "data:application/octet-stream;base64," + Base64.getEncoder().encodeToString(bytes)
}
