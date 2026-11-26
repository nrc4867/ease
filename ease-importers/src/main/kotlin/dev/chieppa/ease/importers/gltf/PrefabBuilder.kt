package dev.chieppa.ease.importers.gltf

import com.github.quillraven.fleks.Component
import dev.chieppa.ease.animation.scene.SkinRef
import dev.chieppa.ease.assets.Guid
import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.gltf.GltfFile
import dev.chieppa.ease.gltf.NodeTransform
import dev.chieppa.ease.gltf.fail
import dev.chieppa.ease.render.scene.Camera
import dev.chieppa.ease.render.scene.DirectionalLight
import dev.chieppa.ease.render.scene.MaterialRef
import dev.chieppa.ease.render.scene.MeshRef
import dev.chieppa.ease.scene.document.SceneDocument
import dev.chieppa.ease.scene.document.SceneNode
import dev.chieppa.ease.scene.node.LocalId
import dev.chieppa.ease.scene.node.OrderKey
import dev.chieppa.ease.scene.slots.AssetRef
import dev.chieppa.ease.scene.types.SceneTypes
import kotlin.math.PI
import kotlinx.serialization.json.JsonObject

private const val DEGREES_PER_RADIAN = (180.0 / PI).toFloat()
private const val ROTATION_W = 3

/**
 * Lux per unit of a `DirectionalLight`'s intensity. The engine's intensity is a sun's irradiance in
 * watts per square metre, as Blender's sun strength is; glTF gives illuminance in lux, which is
 * that times the luminous efficacy of 683 lumens per watt. The default intensity of 3 is about
 * 2,000 lux.
 */
private const val LUX_PER_INTENSITY = 683f

/** The cooked names of one mesh's primitives and the materials they draw with. */
internal class MeshNames(val meshes: List<String>, val materials: List<String>)

/**
 * Builds the prefab of a glTF file's scene (the file's default scene, else its first): one node per
 * glTF node, with its name, its transform, a mesh and material asset slot for a mesh of one
 * primitive (a mesh of several gets a child node per primitive) with a skin slot when the node
 * names a skin, a camera for a perspective camera and a sun for a directional light. Node ids come
 * from [NodeIds], so they are the same on every import. Orthographic cameras and point and spot
 * lights have no engine component yet and are left out.
 */
internal class PrefabBuilder(
  private val file: GltfFile,
  private val guid: Guid,
  private val meshNames: List<MeshNames>,
  private val skinNames: List<String> = emptyList(),
) {
  private val nodes = mutableListOf<SceneNode>()
  private val visited = HashSet<Int>()

  /** The prefab document. */
  fun build(): SceneDocument {
    val root = file.root
    val sceneIndex = root.scene ?: 0
    val scene = root.scenes.getOrNull(sceneIndex)
    if (scene == null && root.scenes.isNotEmpty()) {
      fail("scene", "names scene $sceneIndex of ${root.scenes.size}")
    }
    val tops = scene?.nodes ?: rootsOf()
    var order: OrderKey? = null
    tops.forEach { node ->
      order = OrderKey.after(order)
      add(node, null, checkNotNull(order), "scenes[$sceneIndex].nodes")
    }
    return SceneDocument(nodes = nodes)
  }

  /** Every node no other node lists as a child, for a file without scenes. */
  private fun rootsOf(): List<Int> {
    val nodes = file.root.nodes
    val children = nodes.flatMap { it.children }.toSet()
    return nodes.indices.filter { it !in children }
  }

  private fun add(index: Int, parent: LocalId?, order: OrderKey, path: String) {
    val json =
      file.root.nodes.getOrNull(index) ?: fail(path, "names node $index of ${file.root.nodes.size}")
    if (!visited.add(index)) fail("nodes[$index]", "is reached twice in the tree")
    val id = NodeIds.node(index)
    val components = sortedMapOf(encode(transformOf(index)))
    json.camera?.let { camera(it, "nodes[$index].camera") }?.let { components += encode(it) }
    file
      .lightOf(index)
      ?.takeIf { it.type == "directional" }
      ?.let { light ->
        components +=
          encode(
            DirectionalLight().also {
              it.red = light.color[0]
              it.green = light.color[1]
              it.blue = light.color[2]
              it.intensity = light.intensity / LUX_PER_INTENSITY
            }
          )
      }
    var childOrder: OrderKey? = null
    json.mesh?.let { mesh ->
      val names =
        meshNames.getOrNull(mesh)
          ?: fail("nodes[$index].mesh", "names mesh $mesh of ${meshNames.size}")
      val skin = json.skin?.let { skin(it, "nodes[$index].skin") }.orEmpty()
      if (names.meshes.size == 1) {
        components += drawing(names, 0) + skin
      } else {
        names.meshes.indices.forEach { primitive ->
          childOrder = OrderKey.after(childOrder)
          nodes +=
            SceneNode(
              NodeIds.primitive(index, primitive),
              names.meshes[primitive].substringBeforeLast('.'),
              id,
              checkNotNull(childOrder),
              sortedMapOf(encode(LocalTransform())) + drawing(names, primitive) + skin,
            )
        }
      }
    }
    nodes += SceneNode(id, json.name ?: "node$index", parent, order, components)
    json.children.forEach { child ->
      childOrder = OrderKey.after(childOrder)
      add(child, id, checkNotNull(childOrder), "nodes[$index].children")
    }
  }

  private fun drawing(names: MeshNames, primitive: Int): Map<String, JsonObject> =
    mapOf(
      encode(MeshRef(AssetRef(guid.named(names.meshes[primitive])))),
      encode(MaterialRef(AssetRef(guid.named(names.materials[primitive])))),
    )

  /** The skin component of a node drawn skinned by skin [skin]. */
  private fun skin(skin: Int, path: String): Map<String, JsonObject> {
    val name = skinNames.getOrNull(skin) ?: fail(path, "names skin $skin of ${skinNames.size}")
    return mapOf(encode(SkinRef(AssetRef(guid.named(name)))))
  }

  private fun transformOf(index: Int): LocalTransform {
    val transform = NodeTransform.of(file.root.nodes[index], index)
    return LocalTransform().also {
      it.x = transform.translation[0]
      it.y = transform.translation[1]
      it.z = transform.translation[2]
      it.rotationX = transform.rotation[0]
      it.rotationY = transform.rotation[1]
      it.rotationZ = transform.rotation[2]
      it.rotationW = transform.rotation[ROTATION_W]
      it.scaleX = transform.scale[0]
      it.scaleY = transform.scale[1]
      it.scaleZ = transform.scale[2]
    }
  }

  private fun camera(index: Int, path: String): Camera? {
    val json =
      file.root.cameras.getOrNull(index)
        ?: fail(path, "names camera $index of ${file.root.cameras.size}")
    val lens = json.perspective ?: return null
    return Camera.of(lens.yfov * DEGREES_PER_RADIAN).also {
      it.near = lens.znear
      it.far = lens.zfar ?: Camera.DEFAULT_FAR
    }
  }

  private fun encode(component: Component<*>): Pair<String, JsonObject> {
    val type = checkNotNull(TYPES.of(component)) { "${component::class} is not a prefab type" }
    return type.serialName to type.encode(component)
  }

  private companion object {
    val TYPES: SceneTypes = SceneTypes.build {
      component(MeshRef, MeshRef.serializer())
      component(MaterialRef, MaterialRef.serializer())
      component(Camera, Camera.serializer())
      component(DirectionalLight, DirectionalLight.serializer())
      component(SkinRef, SkinRef.serializer())
    }
  }
}
