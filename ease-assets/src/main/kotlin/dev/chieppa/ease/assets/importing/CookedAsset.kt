package dev.chieppa.ease.assets.importing

/** One file an importer produces: a [name] (no folders) and its [bytes]. */
public class CookedAsset(public val name: String, public val bytes: ByteArray) {
  init {
    require(name.isNotEmpty() && '/' !in name && '\\' !in name && !name.startsWith(".")) {
      "A cooked asset's name is a plain file name, not '$name'"
    }
  }
}
