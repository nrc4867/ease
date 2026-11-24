package dev.chieppa.ease.codegen.output

/**
 * Where the processor writes what other tools read, and how it marks the blocks of the TypeScript
 * part. A build tool shares no code with its readers, so `GeneratedFiles` in `ease-annotations` and
 * `TypeFragments` in the build logic each keep a copy of these names; a test holds them equal.
 */
internal object ResourceFiles {
  /** The resource directory both files are in. */
  const val DIRECTORY = "META-INF/ease"

  /** The JSON Schemas of the module's serializable types, by serial name. */
  const val SCHEMAS_FILE = "schemas.json"

  /** The module's part of `ease.d.ts`. */
  const val TYPES_FILE = "types.d.ts"

  /** Starts the block of a type the module declares, followed by its serial name. */
  const val DECLARED_MARKER = "// ease-declared: "

  /** Starts the block of a type the module only reaches, followed by its serial name. */
  const val REACHED_MARKER = "// ease-reached: "
}
