package dev.chieppa.ease.importers.gltf

import dev.chieppa.ease.animation.assets.CLIP_EXTENSION
import dev.chieppa.ease.animation.assets.ClipFormat
import dev.chieppa.ease.animation.assets.SKIN_EXTENSION
import dev.chieppa.ease.animation.assets.SkinFormat
import dev.chieppa.ease.assets.folders.FolderPack
import dev.chieppa.ease.assets.importing.CookedAsset
import dev.chieppa.ease.assets.importing.ImportContext
import dev.chieppa.ease.assets.importing.ImportException
import dev.chieppa.ease.assets.importing.Importer
import dev.chieppa.ease.gltf.GltfException
import dev.chieppa.ease.gltf.GltfFile
import dev.chieppa.ease.gltf.SiblingFiles
import dev.chieppa.ease.gltf.fail
import dev.chieppa.ease.render.assets.MATERIAL_EXTENSION
import dev.chieppa.ease.render.assets.MESH_EXTENSION
import dev.chieppa.ease.render.assets.MaterialFormat
import dev.chieppa.ease.render.assets.MeshFormat
import dev.chieppa.ease.scene.files.SceneFiles
import dev.chieppa.ease.scene.files.SceneFormat
import kotlinx.serialization.json.JsonObject

/**
 * Imports glTF 2.0 files (`.gltf` with its buffers and images beside it or in data URIs, and
 * `.glb`) into cooked assets, each a sub-asset of the file: a `.mesh` per primitive, a `.material`
 * per material (and `default.material` for primitives that name none), a `.ktx2` texture per image
 * and use (cooked by [textures]; none without it), a `.skin` per skin, a `.clip` per animation
 * (with the events its settings give it; see [ClipSettings]), and a `.prefab` of the scene's node
 * tree whose nodes draw those meshes with those materials, skinned by those skins. Placing the
 * file's own GUID as a prefab instance places that prefab. Names come from the file's own names in
 * file order; every failure names the glTF element at fault.
 */
public class GltfImporter(private val textures: TextureCooker? = null) : Importer {
  override val id: String = ID

  /**
   * [VERSION] twice over, plus one when [textures] cooks images: the cache keeps results made with
   * and without a texture cooker apart, so gaining one imports every glTF file again.
   */
  override val version: Int = VERSION * 2 + if (textures == null) 0 else 1

  override fun import(source: ByteArray, settings: JsonObject): List<CookedAsset> =
    throw ImportException("glTF imports need their import context, for the files beside them")

  override fun import(
    source: ByteArray,
    settings: JsonObject,
    context: ImportContext,
  ): List<CookedAsset> =
    try {
      cook(source, ClipSettings(settings), context)
    } catch (failure: GltfException) {
      throw ImportException("${context.sourceName}: ${failure.message}", failure)
    }

  private fun cook(
    source: ByteArray,
    clips: ClipSettings,
    context: ImportContext,
  ): List<CookedAsset> {
    val files = SiblingFiles { context.map(it).whole }
    val file = GltfFile.read(source, context.sourceName, files)
    val root = file.root
    val names = CookedNames()
    val out = mutableListOf<CookedAsset>()
    val plan = TexturePlan(file, textures, names)
    val materials = MaterialCooker(file, context.guid, plan)
    val materialNames =
      root.materials.indices.map { index ->
        names.take(root.materials[index].name, "material$index", MATERIAL_EXTENSION).also {
          out += CookedAsset(it, MaterialFormat.write(materials.cook(index)))
        }
      }
    val defaultMaterial by lazy {
      names.take("default", "default", MATERIAL_EXTENSION).also {
        out += CookedAsset(it, MaterialFormat.write(MaterialCooker.DEFAULT))
      }
    }
    val primitives = PrimitiveCooker(file)
    val meshNames =
      root.meshes.mapIndexed { m, mesh ->
        val single = mesh.primitives.size == 1
        val cooked =
          mesh.primitives.indices.map { p ->
            val base = mesh.name ?: "mesh$m"
            val name = names.take(if (single) base else "$base-$p", "mesh$m-$p", MESH_EXTENSION)
            out += CookedAsset(name, MeshFormat.write(primitives.cook(m, p)))
            name
          }
        val drawnWith =
          mesh.primitives.mapIndexed { p, primitive ->
            primitive.material?.let { index ->
              materialNames.getOrNull(index)
                ?: fail(
                  "meshes[$m].primitives[$p].material",
                  "names material $index",
                )
            } ?: defaultMaterial
          }
        MeshNames(cooked, drawnWith)
      }
    out += plan.cook(files)
    val skinNames = cookAnimation(file, names, clips, out)
    val prefab = PrefabBuilder(file, context.guid, meshNames, skinNames).build()
    val stem = context.sourceName.substringBeforeLast('.')
    val prefabName = names.take(stem, "model", SceneFormat.PREFAB_EXTENSION)
    val packed = SceneFiles.encode(prefab).mapValues { it.value.toByteArray(Charsets.UTF_8) }
    out += CookedAsset(prefabName, FolderPack.pack(packed))
    return out
  }

  /**
   * Cooks [file]'s skins and its animations as clips with the events [clips] gives them into [out],
   * naming each through [names], and returns the skins' cooked names.
   */
  private fun cookAnimation(
    file: GltfFile,
    names: CookedNames,
    clips: ClipSettings,
    out: MutableList<CookedAsset>,
  ): List<String> {
    val root = file.root
    val animation = AnimationCooker(file)
    val skinNames =
      root.skins.indices.map { index ->
        names.take(root.skins[index].name, "skin$index", SKIN_EXTENSION).also {
          out += CookedAsset(it, SkinFormat.write(animation.skin(index)))
        }
      }
    val clipNames =
      root.animations.indices.map { index ->
        val name = names.take(root.animations[index].name, "clip$index", CLIP_EXTENSION)
        val stem = name.substringBeforeLast('.')
        out += CookedAsset(name, ClipFormat.write(animation.clip(index, stem, clips.events(stem))))
        stem
      }
    clips.checkNames(clipNames)
    return skinNames
  }

  public companion object {
    /** The importer's id, which a sidecar or preset names to choose it. */
    public const val ID: String = "gltf"

    /** Changes whenever the cooked bytes for the same file would change. */
    public const val VERSION: Int = 2
  }
}
