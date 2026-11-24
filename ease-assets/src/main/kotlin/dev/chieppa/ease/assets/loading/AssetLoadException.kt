package dev.chieppa.ease.assets.loading

/** Thrown when an asset cannot be loaded the first time; the message names the asset. */
public class AssetLoadException(message: String, cause: Throwable? = null) :
  RuntimeException(message, cause)
