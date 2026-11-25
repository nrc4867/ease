package dev.chieppa.ease.project.git

/** The `.gitattributes` a new project starts with. */
public object GitAttributesFile {
  /** The file's name. */
  public const val NAME: String = ".gitattributes"

  /** The name of the merge driver in the local git config and the `merge=` attribute. */
  public const val MERGE_DRIVER: String = "ease"

  /** The `.gitattributes` text: LF line endings, LFS for [BinaryAssets], the merge driver. */
  public fun text(): String = buildString {
    appendLine("# Text is stored with LF line endings; scripts for Windows keep CRLF.")
    appendLine("* text=auto eol=lf")
    appendLine("*.bat text eol=crlf")
    appendLine("*.jar binary")
    appendLine()
    appendLine("# Binary sources are in Git LFS and lockable: they cannot be merged, so lock one")
    appendLine("# before editing it.")
    BinaryAssets.EXTENSIONS.forEach {
      appendLine("*.$it filter=lfs diff=lfs merge=lfs -text lockable")
    }
    appendLine()
    appendLine(
      "# Scenes and prefabs merge node by node and property by property (ease merge-driver)."
    )
    appendLine("# The driver is registered in each clone's local git config.")
    appendLine("**/*.scene/**/*.yaml merge=$MERGE_DRIVER")
    appendLine("**/*.prefab/**/*.yaml merge=$MERGE_DRIVER")
  }
}
