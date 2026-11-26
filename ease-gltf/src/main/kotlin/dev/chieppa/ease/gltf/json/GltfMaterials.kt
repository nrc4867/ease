package dev.chieppa.ease.gltf.json

import dev.chieppa.ease.annotations.Doc
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * A material in the metallic-roughness model, with its normal, occlusion and emissive textures and
 * how its alpha is used (`OPAQUE`, `MASK` with [alphaCutoff], or `BLEND`).
 */
@Doc(
  "A material in the metallic-roughness model, with its normal, occlusion and emissive " +
    "textures and how its alpha is used (OPAQUE, MASK with alphaCutoff, or BLEND)."
)
@Serializable
@Suppress("LongParameterList") // The glTF object's properties, as the specification names them.
public class MaterialJson(
  @Doc("The glTF property name.") public val name: String? = null,
  @Doc("The glTF property pbrMetallicRoughness.")
  public val pbrMetallicRoughness: PbrJson = PbrJson(),
  @Doc("The glTF property normalTexture.") public val normalTexture: TextureRefJson? = null,
  @Doc("The glTF property occlusionTexture.") public val occlusionTexture: TextureRefJson? = null,
  @Doc("The glTF property emissiveTexture.") public val emissiveTexture: TextureRefJson? = null,
  @Doc("The glTF property emissiveFactor.")
  public val emissiveFactor: List<Float> = listOf(0f, 0f, 0f),
  @Doc("The glTF property alphaMode.") public val alphaMode: String = "OPAQUE",
  @Doc("The glTF property alphaCutoff.") public val alphaCutoff: Float = DEFAULT_ALPHA_CUTOFF,
  @Doc("The glTF property doubleSided.") public val doubleSided: Boolean = false,
  @Doc("The glTF property extensions.") public val extensions: JsonObject? = null,
) {
  public companion object {
    /** The alpha below which a `MASK` material is cut away, unless the file says otherwise. */
    public const val DEFAULT_ALPHA_CUTOFF: Float = 0.5f
  }
}

/** Base color, metallic and roughness factors and textures; factors multiply the textures. */
@Doc("Base color, metallic and roughness factors and textures; factors multiply the textures.")
@Serializable
public class PbrJson(
  @Doc("The glTF property baseColorFactor.")
  public val baseColorFactor: List<Float> = listOf(1f, 1f, 1f, 1f),
  @Doc("The glTF property baseColorTexture.") public val baseColorTexture: TextureRefJson? = null,
  @Doc("The glTF property metallicFactor.") public val metallicFactor: Float = 1f,
  @Doc("The glTF property roughnessFactor.") public val roughnessFactor: Float = 1f,
  @Doc("The glTF property metallicRoughnessTexture.")
  public val metallicRoughnessTexture: TextureRefJson? = null,
)

/**
 * A material's use of a texture: which [index], which texture coordinates ([texCoord]), and the
 * normal map's [scale] or the occlusion's [strength].
 */
@Doc(
  "A material's use of a texture: which index, which texture coordinates (texCoord), and " +
    "the normal map's scale or the occlusion's strength."
)
@Serializable
public class TextureRefJson(
  @Doc("The glTF property index.") public val index: Int,
  @Doc("The glTF property texCoord.") public val texCoord: Int = 0,
  @Doc("The glTF property scale.") public val scale: Float = 1f,
  @Doc("The glTF property strength.") public val strength: Float = 1f,
)

/** A texture: an image and how it is sampled. */
@Doc("A texture: an image and how it is sampled.")
@Serializable
public class TextureJson(
  @Doc("The glTF property source.") public val source: Int? = null,
  @Doc("The glTF property sampler.") public val sampler: Int? = null,
  @Doc("The glTF property name.") public val name: String? = null,
)

/** An image: at [uri], or in a [bufferView] with a [mimeType]. */
@Doc("An image: at uri, or in a bufferView with a mimeType.")
@Serializable
public class ImageJson(
  @Doc("The glTF property uri.") public val uri: String? = null,
  @Doc("The glTF property mimeType.") public val mimeType: String? = null,
  @Doc("The glTF property bufferView.") public val bufferView: Int? = null,
  @Doc("The glTF property name.") public val name: String? = null,
)

/** Filters and wrap modes, as the WebGL constants glTF uses. */
@Doc("Filters and wrap modes, as the WebGL constants glTF uses.")
@Serializable
public class SamplerJson(
  @Doc("The glTF property magFilter.") public val magFilter: Int? = null,
  @Doc("The glTF property minFilter.") public val minFilter: Int? = null,
  @Doc("The glTF property wrapS.") public val wrapS: Int = REPEAT,
  @Doc("The glTF property wrapT.") public val wrapT: Int = REPEAT,
) {
  public companion object {
    /** Wrapping that repeats the texture, the default. */
    public const val REPEAT: Int = 10497
  }
}
