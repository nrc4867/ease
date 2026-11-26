package dev.chieppa.ease.assets.importing

import dev.chieppa.ease.assets.associations.Cardinality
import dev.chieppa.ease.assets.associations.Contributor
import dev.chieppa.ease.assets.associations.ContributorKind
import kotlinx.serialization.json.JsonObject

/**
 * Turns a source file into cooked assets. Importers register with the file association service
 * under [Importer.kind]; a file takes exactly one. The result depends on nothing but the source
 * bytes, the settings and [version]: the same inputs give the same bytes on every machine, so
 * results can be cached by a hash of them and checked in CI. An importer reads other files only
 * through its [ImportContext], which records them as inputs, and keeps no state between calls.
 */
public interface Importer : Contributor {
  /** The name a sidecar or folder preset uses to choose this importer over the matching one. */
  public val id: String

  /** Changes whenever the importer would produce different bytes for the same inputs. */
  public val version: Int

  /**
   * The cooked assets for [source] under [settings], in a stable order. Throws [ImportException]
   * (or any `RuntimeException`) when the source is not valid; the asset then keeps its last good
   * result.
   */
  public fun import(source: ByteArray, settings: JsonObject): List<CookedAsset>

  /**
   * The cooked assets for [source] under [settings], with [context] for the source's GUID and the
   * files beside it. The import service calls this one; by default it ignores [context].
   */
  public fun import(
    source: ByteArray,
    settings: JsonObject,
    context: ImportContext,
  ): List<CookedAsset> = import(source, settings)

  public companion object {
    /** The contributor kind of importers: one per file, overridable in the sidecar or a preset. */
    public val kind: ContributorKind<Importer> =
      ContributorKind("Importer", Importer::class, Cardinality.ONE)
  }
}
