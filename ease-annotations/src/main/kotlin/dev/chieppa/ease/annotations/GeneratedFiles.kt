package dev.chieppa.ease.annotations

/**
 * Where `ease-codegen` writes what it generates, under each module's resources, and the lines that
 * start each block of a module's part of `ease.d.ts`. Code that reads them at runtime or in tests
 * takes the names from here; the build tool and the build logic cannot depend on this module, so
 * each keeps a copy that a test holds equal.
 */
public object GeneratedFiles {
  /** The resource directory both files are in. */
  public const val DIRECTORY: String = "META-INF/ease"

  /** The JSON Schemas of a module's serializable types, by serial name. */
  public const val SCHEMAS_FILE: String = "schemas.json"

  /** A module's part of `ease.d.ts`. */
  public const val TYPES_FILE: String = "types.d.ts"

  /** Starts the block of a type the module declares, followed by its serial name. */
  public const val DECLARED_MARKER: String = "// ease-declared: "

  /** Starts the block of a type the module only reaches, followed by its serial name. */
  public const val REACHED_MARKER: String = "// ease-reached: "
}
