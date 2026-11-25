package dev.chieppa.ease.cli.template

import dev.chieppa.ease.cli.CliFailure

/**
 * The simple names the templates import or the language gives every file. A generated type with one
 * of them would bind to the engine's type of that name, or fail to compile.
 */
public object ReservedNames {
  private val TAKEN =
    setOf(
      "Any",
      "Component",
      "ComponentType",
      "CommandRegistry",
      "Doc",
      "Entity",
      "Float",
      "GameModule",
      "GameModules",
      "HostedWorld",
      "Int",
      "IteratingSystem",
      "List",
      "LocalTransform",
      "Phase",
      "PhasedSystems",
      "Quad",
      "SerialName",
      "Serializable",
      "Spawn",
      "String",
      "Test",
      "Unit",
    )

  /** Fails when [typeName], or [typeName] followed by [suffix], is a reserved name. */
  public fun requireFree(typeName: String, suffix: String = "") {
    val clash = listOf(typeName, typeName + suffix).firstOrNull { it in TAKEN } ?: return
    throw CliFailure("$clash is a name the engine or the templates already use: pick another")
  }
}
