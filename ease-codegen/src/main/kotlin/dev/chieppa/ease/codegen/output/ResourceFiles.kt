package dev.chieppa.ease.codegen.output

/**
 * Where the processor writes what other tools read, under each module's resources. `ease-agent`
 * loads the schemas from the classpath and the game's build joins the TypeScript fragments; each
 * names these paths itself, since a build tool shares no code with them.
 */
internal object ResourceFiles {
  /** The directory both files are in. */
  const val DIRECTORY = "META-INF.ease"

  /** The JSON Schemas of the module's serializable types, by serial name. */
  const val SCHEMAS = "schemas"

  /** The module's part of `ease.d.ts`. */
  const val TYPES = "types"

  const val JSON = "json"
  const val DECLARATIONS = "d.ts"
}
