package dev.chieppa.ease.scene.files

/** Thrown when a scene or prefab file cannot be read; the message names the file. */
public class SceneFileException(public val file: String, reason: String, cause: Throwable? = null) :
  RuntimeException("Scene file $file: $reason", cause)
