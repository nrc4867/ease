package dev.chieppa.ease.serialization.yaml

/** Thrown when text is not YAML the canonical format reads. */
public class YamlFormatException(message: String, cause: Throwable? = null) :
  RuntimeException(message, cause)
