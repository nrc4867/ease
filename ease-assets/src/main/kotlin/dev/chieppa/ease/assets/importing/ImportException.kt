package dev.chieppa.ease.assets.importing

/** Thrown by an [Importer] for a source it cannot import; the message says what is wrong. */
public class ImportException(message: String, cause: Throwable? = null) :
  RuntimeException(message, cause)
