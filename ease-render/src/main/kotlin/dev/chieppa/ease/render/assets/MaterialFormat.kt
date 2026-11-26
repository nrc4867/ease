package dev.chieppa.ease.render.assets

import dev.chieppa.ease.assets.Guid
import dev.chieppa.ease.assets.loading.AssetLoader
import dev.chieppa.ease.serialization.yaml.CanonicalYaml
import kotlin.reflect.KClass
import kotlinx.serialization.json.Json

/**
 * The material asset file (`.material`): a [MaterialAsset] as canonical YAML, every field written,
 * so the same material always gives the same bytes.
 */
public object MaterialFormat : AssetLoader<MaterialAsset> {
  private val json = Json {
    encodeDefaults = true
    ignoreUnknownKeys = false
  }

  override val type: KClass<MaterialAsset> = MaterialAsset::class

  /** The bytes of [material]. */
  public fun write(material: MaterialAsset): ByteArray =
    CanonicalYaml.write(json.encodeToJsonElement(MaterialAsset.serializer(), material))
      .toByteArray(Charsets.UTF_8)

  /** The material [bytes] hold; throws when they are not one. */
  public fun read(bytes: ByteArray): MaterialAsset =
    json.decodeFromJsonElement(
      MaterialAsset.serializer(),
      CanonicalYaml.read(bytes.toString(Charsets.UTF_8)),
    )

  override fun load(guid: Guid, bytes: ByteArray): MaterialAsset = read(bytes)
}
