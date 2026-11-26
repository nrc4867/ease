package dev.chieppa.ease.importers.gltf

import dev.chieppa.ease.animation.assets.ChannelPath
import dev.chieppa.ease.animation.assets.ClipAsset
import dev.chieppa.ease.animation.assets.SkinAsset
import dev.chieppa.ease.render.assets.MaterialAsset
import dev.chieppa.ease.render.assets.MeshAsset
import dev.chieppa.ease.render.scene.MeshRef
import dev.chieppa.ease.scene.document.SceneDocument
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import org.junit.jupiter.api.io.TempDir

/** glTF files imported through the import service and loaded back through the asset store. */
class GltfImporterTest {
  @TempDir lateinit var folder: Path

  private val project by lazy { ImportedProject(folder) }

  @Test
  fun `a Blender export cooks a mesh per primitive, its material and a prefab of its nodes`() {
    val car = project.add("models/car_sedan.gltf")
    project.add("models/car_sedan.bin")
    project.add("models/citybits_texture.png")
    val (store, imports) = project.open()

    assertEquals(emptyList(), imports.problems.map { it.toString() })
    val names = imports.cookedNames(car)
    assertTrue("car_sedan.prefab" in names, "$names")
    assertTrue("citybits_texture.material" in names, "$names")
    assertEquals(5, names.count { it.endsWith(".mesh") }, "$names")

    val material = store.handle(car.named("citybits_texture.material"), MaterialAsset::class).value
    assertEquals(0f, material.metallic)
    assertEquals(0.4f, material.roughness, 1e-6f)

    val prefab = store.handle(car, SceneDocument::class).value
    val body = prefab.nodes.values.single { it.name == "car_sedan" }
    val wheels = prefab.children(body.id)
    assertEquals(4, wheels.size)
    val meshGuid = (body.components.getValue(MESH_REF)["mesh"] as JsonPrimitive).content
    val mesh = store.handle(dev.chieppa.ease.assets.Guid.parse(meshGuid), MeshAsset::class).value
    assertTrue(mesh.mesh.indexCount > 0 && mesh.mesh.indexCount % 3 == 0)
    val wheel = wheels.first { it.name.endsWith("front_left") }
    val x = (wheel.components.getValue("ease.LocalTransform")["x"] as JsonPrimitive).content
    assertEquals(0.17599869f, x.toFloat(), 1e-6f)
  }

  @Test
  fun `skins and animations cook as skins and clips naming the prefab's nodes`() {
    val skinned = project.add("khronos/SimpleSkin.gltf")
    val (store, _) = project.open()

    val skin = store.handle(skinned.named("skin0.skin"), SkinAsset::class).value
    val clip = store.handle(skinned.named("clip0.clip"), ClipAsset::class).value
    val prefab = store.handle(skinned, SceneDocument::class).value

    assertEquals(listOf(NodeIds.node(1).text, NodeIds.node(2).text), skin.joints)
    assertEquals(-1f, skin.inverseBindMatrices[16 + 13])
    assertEquals(ChannelPath.ROTATION, clip.channels.single().path)
    assertEquals(NodeIds.node(2).text, clip.channels.single().node)
    assertEquals(5.5f, clip.duration)
    assertTrue(prefab.nodes.containsKey(NodeIds.node(2)))
  }

  @Test
  fun `a skinned mesh's node gets its skin, and clip events come from the import settings`() {
    val events = buildJsonObject {
      putJsonObject("clips") {
        putJsonObject("clip0") {
          putJsonArray("events") {
            addJsonObject {
              put("name", "wave")
              put("time", 2.5f)
            }
            addJsonObject {
              put("name", "rest")
              put("time", 0.5f)
            }
          }
        }
      }
    }
    val skinned = project.add("khronos/SimpleSkin.gltf", settings = events)
    val (store, imports) = project.open()

    assertEquals(emptyList(), imports.problems.map { it.toString() })
    val clip = store.handle(skinned.named("clip0.clip"), ClipAsset::class).value
    assertEquals(listOf("rest" to 0.5f, "wave" to 2.5f), clip.events.map { it.name to it.time })
    val prefab = store.handle(skinned, SceneDocument::class).value
    val skin = prefab[NodeIds.node(0)]?.components?.get("ease.SkinRef")
    assertEquals(
      skinned.named("skin0.skin").toString(),
      (skin?.get("skin") as JsonPrimitive).content,
    )
  }

  @Test
  fun `clip settings naming a clip the file lacks fail the import`() {
    val settings = buildJsonObject { putJsonObject("clips") { putJsonObject("Jump") {} } }
    project.add("khronos/SimpleSkin.gltf", settings = settings)
    val (_, imports) = project.open()

    val problem = imports.problems.single().message
    assertTrue("Jump" in problem, problem)
  }

  @Test
  fun `a clip event set past the clip's end fails the import, naming its setting`() {
    val settings = buildJsonObject {
      putJsonObject("clips") {
        putJsonObject("clip0") {
          putJsonArray("events") {
            addJsonObject {
              put("name", "late")
              put("time", 60f)
            }
          }
        }
      }
    }
    project.add("khronos/SimpleSkin.gltf", settings = settings)
    val (_, imports) = project.open()

    val problem = imports.problems.single().message
    assertTrue("settings.clips.clip0.events[0].time" in problem, problem)
  }

  @Test
  fun `a GLB imports from its binary chunk`() {
    val box = project.add("khronos/BoxVertexColors.glb")
    val (store, imports) = project.open()

    assertEquals(emptyList(), imports.problems.map { it.toString() })
    val mesh = store.handle(box.named("mesh0.mesh"), MeshAsset::class).value
    assertEquals(36, mesh.mesh.indexCount)
    assertEquals(24 * 4, mesh.colors?.size)
    assertContentEquals(floatArrayOf(0f, 0f, 0f), mesh.boundsMin)
  }

  @Test
  fun `a primitive that is not a triangle list is refused by its path`() {
    project.add("khronos/MeshPrimitiveModes.gltf")
    val (_, imports) = project.open()

    val problem = imports.problems.single().message
    assertTrue("meshes[0].primitives[0].mode" in problem, problem)
  }

  @Test
  fun `a perspective camera becomes a camera in degrees`() {
    val cameras = project.add("khronos/Cameras.gltf")
    val (store, _) = project.open()

    val prefab = store.handle(cameras, SceneDocument::class).value
    val camera = prefab[NodeIds.node(1)]?.components?.get("ease.Camera")
    val degrees = (camera?.get("fieldOfView") as JsonPrimitive).content.toFloat()
    assertEquals(40.107f, degrees, 1e-3f)
    assertEquals(null, prefab[NodeIds.node(2)]?.components?.get("ease.Camera"))
  }

  @Test
  fun `unindexed vertices that are not whole triangles are refused by their path`() {
    project.write("loose.gltf", LOOSE_VERTICES)
    val (_, imports) = project.open()

    val problem = imports.problems.single().message
    assertTrue("meshes[0].primitives[0].attributes.POSITION" in problem, problem)
  }

  @Test
  fun `a directional light's lux becomes the engine's intensity at 683 lux a unit`() {
    val lit = project.write("sun.gltf", SUN)
    val (store, _) = project.open()

    val prefab = store.handle(lit, SceneDocument::class).value
    val light = prefab[NodeIds.node(0)]?.components?.get("ease.DirectionalLight")
    val intensity = (light?.get("intensity") as JsonPrimitive).content.toFloat()
    assertEquals(3f, intensity, 1e-4f)
  }

  @Test
  fun `having a texture cooker changes the importer's version, so results are kept apart`() {
    val without = GltfImporter()
    val with = GltfImporter { _, _, _ -> ByteArray(0) }

    assertTrue(without.version != with.version)
  }

  private companion object {
    /** Four vertices drawn as a triangle list with no indices. */
    val LOOSE_VERTICES: String =
      """
      {"asset": {"version": "2.0"},
       "buffers": [{"byteLength": 48, "uri": "data:application/octet-stream;base64,AAAAAAAAAAAAAAAAAACAPwAAAAAAAAAAAAAAAAAAgD8AAAAAAACAPwAAgD8AAAAA"}],
       "bufferViews": [{"buffer": 0, "byteLength": 48}],
       "accessors": [{"bufferView": 0, "componentType": 5126, "count": 4, "type": "VEC3"}],
       "meshes": [{"primitives": [{"attributes": {"POSITION": 0}}]}],
       "nodes": [{"mesh": 0}]}
      """
        .trimIndent()

    /** One node holding a sun of 2,049 lux. */
    val SUN: String =
      """
      {"asset": {"version": "2.0"}, "extensionsUsed": ["KHR_lights_punctual"],
       "extensions": {"KHR_lights_punctual": {"lights": [{"type": "directional", "intensity": 2049}]}},
       "nodes": [{"extensions": {"KHR_lights_punctual": {"light": 0}}}]}
      """
        .trimIndent()

    val MESH_REF: String = MeshRef.serializer().descriptor.serialName
  }
}
