package dev.chieppa.ease.assets.associations

/**
 * Says that a folder its match names is one asset, as a scene folder is: the folder has the meta
 * file beside it, and the files inside it are its contents, not assets of their own. A module
 * registers one per folder format, matching the folder's name, such as the extension `scene` for
 * `cave.scene/`.
 */
public interface FolderAsset : Contributor {
  public companion object {
    /** The contributor kind of folder formats; any number may match, and one is enough. */
    public val kind: ContributorKind<FolderAsset> =
      ContributorKind("Folder asset", FolderAsset::class, Cardinality.MANY)
  }
}
