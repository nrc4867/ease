package dev.chieppa.ease.gltf

/**
 * Thrown when a glTF file cannot be read as the specification says. [element] is the JSON path of
 * what is wrong, such as `accessors[3].sparse.indices` or `meshes[0].primitives[1].mode`, and the
 * message starts with it.
 */
public class GltfException(public val element: String, problem: String, cause: Throwable? = null) :
  RuntimeException("$element: $problem", cause)

/** Throws a [GltfException] about [element]: how reading code reports what it found wrong. */
public fun fail(element: String, problem: String): Nothing = throw GltfException(element, problem)
