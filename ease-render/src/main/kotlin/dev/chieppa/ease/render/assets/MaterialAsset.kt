package dev.chieppa.ease.render.assets

import dev.chieppa.ease.annotations.Doc
import dev.chieppa.ease.assets.Guid
import kotlinx.serialization.Serializable

@Doc(
  "A lit material asset: the lit template's parameters and the textures that scale them, each " +
    "texture an asset by GUID. Colors are linear; textures multiply the factors, as glTF says."
)
@Serializable
@Suppress("LongParameterList") // One field a property, as the file writes them.
public class MaterialAsset(
  @Doc("Base color red, green, blue and alpha, linear, 0 to 1.")
  public val baseColor: List<Float> = listOf(1f, 1f, 1f, 1f),
  @Doc("How metallic the surface is: 0 is a dielectric, 1 a metal.")
  public val metallic: Float = 1f,
  @Doc("How rough the surface is: 0 is mirror-smooth, 1 fully diffuse.")
  public val roughness: Float = 1f,
  @Doc("Emitted light, red, green and blue, linear.")
  public val emissive: List<Float> = listOf(0f, 0f, 0f),
  @Doc("How alpha is used: opaque, cut out below alphaCutoff, or blended.")
  public val alphaMode: AlphaMode = AlphaMode.OPAQUE,
  @Doc("The alpha below which a MASK material is cut away.") public val alphaCutoff: Float = 0.5f,
  @Doc("Whether back faces are drawn too.") public val doubleSided: Boolean = false,
  @Doc("The base color texture, in sRGB; scales baseColor.")
  public val baseColorTexture: TextureSlot? = null,
  @Doc("Roughness in green and metallic in blue, linear; scale roughness and metallic.")
  public val metallicRoughnessTexture: TextureSlot? = null,
  @Doc("The tangent-space normal map; its scale bends the normals.")
  public val normalTexture: TextureSlot? = null,
  @Doc("Ambient occlusion in red; its strength says how much.")
  public val occlusionTexture: TextureSlot? = null,
  @Doc("Emitted light, in sRGB; scales emissive.") public val emissiveTexture: TextureSlot? = null,
)

@Doc("How a material's alpha is used.")
@Serializable
public enum class AlphaMode {
  @Doc("Alpha is ignored.") OPAQUE,
  @Doc("Pixels whose alpha is below the cutoff are cut away.") MASK,
  @Doc("The surface is blended over what is behind it.") BLEND,
}

@Doc("A material's use of a texture asset.")
@Serializable
public class TextureSlot(
  @Doc("The texture asset's GUID.") public val texture: Guid,
  @Doc("Which texture coordinates it reads: 0 or 1.") public val texCoord: Int = 0,
  @Doc("The normal map's scale, or the occlusion's strength; 1 for other textures.")
  public val amount: Float = 1f,
)
