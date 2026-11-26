package dev.chieppa.ease.gltf.json

import dev.chieppa.ease.annotations.Doc
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * A glTF 2.0 file's JSON, with the specification's names. Lists the file leaves out are empty and
 * indices into them stay plain integers, which the reader checks against each list's size before
 * use. Unknown properties are ignored; [extensions] keeps the root's extensions as written.
 */
@Doc(
  "A glTF 2.0 file's JSON, with the specification's names. Lists the file leaves out are " +
    "empty and indices into them stay plain integers, which the reader checks against each " +
    "list's size before use. Unknown properties are ignored; extensions keeps the root's " +
    "extensions as written."
)
@Serializable
@Suppress("LongParameterList") // The glTF object's properties, as the specification names them.
public class GltfRoot(
  @Doc("The glTF property asset.") public val asset: AssetInfo,
  @Doc("The glTF property scene.") public val scene: Int? = null,
  @Doc("The glTF property scenes.") public val scenes: List<SceneJson> = emptyList(),
  @Doc("The glTF property nodes.") public val nodes: List<NodeJson> = emptyList(),
  @Doc("The glTF property meshes.") public val meshes: List<MeshJson> = emptyList(),
  @Doc("The glTF property accessors.") public val accessors: List<AccessorJson> = emptyList(),
  @Doc("The glTF property bufferViews.") public val bufferViews: List<BufferViewJson> = emptyList(),
  @Doc("The glTF property buffers.") public val buffers: List<BufferJson> = emptyList(),
  @Doc("The glTF property materials.") public val materials: List<MaterialJson> = emptyList(),
  @Doc("The glTF property textures.") public val textures: List<TextureJson> = emptyList(),
  @Doc("The glTF property images.") public val images: List<ImageJson> = emptyList(),
  @Doc("The glTF property samplers.") public val samplers: List<SamplerJson> = emptyList(),
  @Doc("The glTF property skins.") public val skins: List<SkinJson> = emptyList(),
  @Doc("The glTF property animations.") public val animations: List<AnimationJson> = emptyList(),
  @Doc("The glTF property cameras.") public val cameras: List<CameraJson> = emptyList(),
  @Doc("The glTF property extensionsUsed.") public val extensionsUsed: List<String> = emptyList(),
  @Doc("The glTF property extensionsRequired.")
  public val extensionsRequired: List<String> = emptyList(),
  @Doc("The glTF property extensions.") public val extensions: JsonObject? = null,
)

/** The `asset` block: the glTF version and the tool that wrote the file. */
@Doc("The asset block: the glTF version and the tool that wrote the file.")
@Serializable
public class AssetInfo(
  @Doc("The glTF property version.") public val version: String,
  @Doc("The glTF property generator.") public val generator: String? = null,
  @Doc("The glTF property minVersion.") public val minVersion: String? = null,
)

/** A scene: the indices of its root nodes. */
@Doc("A scene: the indices of its root nodes.")
@Serializable
public class SceneJson(
  @Doc("The glTF property name.") public val name: String? = null,
  @Doc("The glTF property nodes.") public val nodes: List<Int> = emptyList(),
)

/**
 * A node: its children, an optional mesh, skin and camera, and its transform as either a
 * column-major [matrix] or [translation], [rotation] (x, y, z, w) and [scale].
 */
@Doc(
  "A node: its children, an optional mesh, skin and camera, and its transform as either a " +
    "column-major matrix or translation, rotation (x, y, z, w) and scale."
)
@Serializable
@Suppress("LongParameterList") // The glTF object's properties, as the specification names them.
public class NodeJson(
  @Doc("The glTF property name.") public val name: String? = null,
  @Doc("The glTF property children.") public val children: List<Int> = emptyList(),
  @Doc("The glTF property mesh.") public val mesh: Int? = null,
  @Doc("The glTF property skin.") public val skin: Int? = null,
  @Doc("The glTF property camera.") public val camera: Int? = null,
  @Doc("The glTF property matrix.") public val matrix: List<Float>? = null,
  @Doc("The glTF property translation.") public val translation: List<Float>? = null,
  @Doc("The glTF property rotation.") public val rotation: List<Float>? = null,
  @Doc("The glTF property scale.") public val scale: List<Float>? = null,
  @Doc("The glTF property extensions.") public val extensions: JsonObject? = null,
)
