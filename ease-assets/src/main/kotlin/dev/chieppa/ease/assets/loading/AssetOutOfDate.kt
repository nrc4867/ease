package dev.chieppa.ease.assets.loading

/**
 * Thrown by a reload when this machine's copy of the file is not the version the reload names, so
 * the world stays as it was.
 */
public class AssetOutOfDate(message: String) : RuntimeException(message)
