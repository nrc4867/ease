package dev.chieppa.ease.importers.gltf

import dev.chieppa.ease.assets.Guid
import dev.chieppa.ease.gltf.GltfFile
import dev.chieppa.ease.gltf.fail
import dev.chieppa.ease.gltf.json.MaterialJson
import dev.chieppa.ease.gltf.json.TextureRefJson
import dev.chieppa.ease.render.assets.AlphaMode
import dev.chieppa.ease.render.assets.MaterialAsset
import dev.chieppa.ease.render.assets.TextureSlot
import dev.chieppa.ease.textures.TextureUsage
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.floatOrNull

private const val EMISSIVE_STRENGTH = "KHR_materials_emissive_strength"
private const val COLOR_CHANNELS = 3
private const val RGBA = 4

/**
 * Turns glTF materials into material assets. Each texture slot names the texture sub-asset cooked
 * from its image for that use (sRGB color, linear data or a normal map), by the source's [guid]
 * named by the texture's cooked name; [textures] decides those names. Emissive strength
 * (`KHR_materials_emissive_strength`) scales the emissive color.
 */
internal class MaterialCooker(
  private val file: GltfFile,
  private val guid: Guid,
  private val textures: TexturePlan,
) {
  /** The asset of material [index]. */
  fun cook(index: Int): MaterialAsset {
    val path = "materials[$index]"
    val material = file.root.materials[index]
    val pbr = material.pbrMetallicRoughness
    if (pbr.baseColorFactor.size != RGBA) {
      fail("$path.pbrMetallicRoughness.baseColorFactor", "needs 4 numbers")
    }
    if (material.emissiveFactor.size != COLOR_CHANNELS) {
      fail("$path.emissiveFactor", "needs 3 numbers")
    }
    val strength = emissiveStrength(material)
    return MaterialAsset(
      baseColor = pbr.baseColorFactor,
      metallic = pbr.metallicFactor,
      roughness = pbr.roughnessFactor,
      emissive = material.emissiveFactor.map { it * strength },
      alphaMode = alphaMode(material, path),
      alphaCutoff = material.alphaCutoff,
      doubleSided = material.doubleSided,
      baseColorTexture = slot(pbr.baseColorTexture, TextureUsage.COLOR_SRGB, 1f),
      metallicRoughnessTexture = slot(pbr.metallicRoughnessTexture, TextureUsage.DATA_LINEAR, 1f),
      normalTexture = material.normalTexture?.let { slot(it, TextureUsage.NORMAL, it.scale) },
      occlusionTexture =
        material.occlusionTexture?.let { slot(it, TextureUsage.DATA_LINEAR, it.strength) },
      emissiveTexture = slot(material.emissiveTexture, TextureUsage.COLOR_SRGB, 1f),
    )
  }

  private fun slot(ref: TextureRefJson?, usage: TextureUsage, amount: Float): TextureSlot? {
    val name = ref?.let { textures.nameOf(it.index, usage) }
    return name?.let { TextureSlot(guid.named(it), checkNotNull(ref).texCoord, amount) }
  }

  private fun alphaMode(material: MaterialJson, path: String): AlphaMode =
    when (material.alphaMode) {
      "OPAQUE" -> AlphaMode.OPAQUE
      "MASK" -> AlphaMode.MASK
      "BLEND" -> AlphaMode.BLEND
      else -> fail("$path.alphaMode", "'${material.alphaMode}' is not glTF's")
    }

  private fun emissiveStrength(material: MaterialJson): Float {
    val extension = material.extensions?.get(EMISSIVE_STRENGTH) as? JsonObject ?: return 1f
    return (extension["emissiveStrength"] as? JsonPrimitive)?.floatOrNull ?: 1f
  }

  companion object {
    /** glTF's default material, for primitives that name none. */
    val DEFAULT: MaterialAsset = MaterialAsset()
  }
}
