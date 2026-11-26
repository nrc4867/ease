package dev.chieppa.ease.animation.assets

import dev.chieppa.ease.assets.Guid
import dev.chieppa.ease.assets.associations.FileAssociations
import dev.chieppa.ease.assets.associations.FileMatch
import dev.chieppa.ease.assets.associations.Registration
import dev.chieppa.ease.assets.loading.AssetLoader
import dev.chieppa.ease.serialization.yaml.CanonicalYaml
import kotlin.reflect.KClass
import kotlinx.serialization.json.Json

/** Where animation asset contributors say they came from. */
private const val ORIGIN = "ease-animation"

/** The priority animation loaders register at. */
private const val PRIORITY = 0

/** The cooked skin extension. */
public const val SKIN_EXTENSION: String = "skin"

/** The cooked animation clip extension. */
public const val CLIP_EXTENSION: String = "clip"

/** The animator asset extension. */
public const val ANIMATOR_EXTENSION: String = "animator"

/**
 * The animator asset file (`.animator`): an [AnimatorAsset] as canonical YAML, every field written,
 * so the same animator always gives the same bytes.
 */
public object AnimatorFormat : AssetLoader<AnimatorAsset> {
  private val json = Json {
    encodeDefaults = true
    ignoreUnknownKeys = false
  }

  override val type: KClass<AnimatorAsset> = AnimatorAsset::class

  /** The bytes of [animator]. */
  public fun write(animator: AnimatorAsset): ByteArray =
    CanonicalYaml.write(json.encodeToJsonElement(AnimatorAsset.serializer(), animator))
      .toByteArray(Charsets.UTF_8)

  /** The animator [bytes] hold; throws when they are not one. */
  public fun read(bytes: ByteArray): AnimatorAsset =
    json.decodeFromJsonElement(
      AnimatorAsset.serializer(),
      CanonicalYaml.read(bytes.toString(Charsets.UTF_8)),
    )

  override fun load(guid: Guid, bytes: ByteArray): AnimatorAsset = read(bytes)
}

/**
 * Registers the loaders of animation assets with the file association service: cooked skins
 * (`.skin`) and clips (`.clip`), and animators (`.animator`), whether they are files of their own
 * or an importer's cooked assets. Close the returned handles to remove them.
 */
public fun FileAssociations.registerAnimationAssets(): List<Registration> =
  listOf<Pair<String, AssetLoader<*>>>(
      SKIN_EXTENSION to SkinFormat,
      CLIP_EXTENSION to ClipFormat,
      ANIMATOR_EXTENSION to AnimatorFormat,
    )
    .map { (extension, loader) ->
      register(AssetLoader.kind, FileMatch.Extension(extension), PRIORITY, loader, ORIGIN)
    }
