package dev.chieppa.ease.build.licenses

import dev.chieppa.ease.build.JoltNatives
import org.gradle.api.Project
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult

/** Reads the licenses of the external modules a resolved dependency graph holds. */
object ShippedModules {
  /** How far up a chain of parent POMs to look for a license. */
  private const val MAX_PARENTS = 5

  /**
   * Every external module in the graph under [root], as `group:name:version`, with jolt-jni's
   * per-platform artifacts named as one (see [JoltNatives.portable]).
   */
  fun coordinates(root: ResolvedComponentResult): List<String> {
    val seen = mutableSetOf<ResolvedComponentResult>()
    val pending = ArrayDeque(listOf(root))
    while (pending.isNotEmpty()) {
      val component = pending.removeFirst()
      if (seen.add(component)) {
        component.dependencies.filterIsInstance<ResolvedDependencyResult>().forEach {
          pending.add(it.selected)
        }
      }
    }
    return seen
      .map { it.id }
      .filterIsInstance<ModuleComponentIdentifier>()
      .map { JoltNatives.portable("${it.group}:${it.module}:${it.version}") }
      .distinct()
      .sorted()
  }

  /**
   * The licenses each module in [coordinates] declares, taken from its POM or, when that declares
   * none, the nearest parent POM that does.
   */
  fun licenses(project: Project, coordinates: List<String>): List<ModuleLicenses> {
    val found = coordinates.associateWith { emptyList<DeclaredLicense>() }.toMutableMap()
    var lookup = coordinates.associateWith { it }
    repeat(MAX_PARENTS + 1) {
      if (lookup.isEmpty()) return@repeat
      val poms = poms(project, lookup.values.toSet())
      val next = mutableMapOf<String, String>()
      lookup.forEach { (module, pom) ->
        val read = poms[pom]
        when {
          read == null -> Unit
          read.licenses.isNotEmpty() -> found[module] = read.licenses
          read.parent != null -> next[module] = read.parent
        }
      }
      lookup = next
    }
    return coordinates.map { coordinate ->
      val (group, name, version) = coordinate.split(":")
      ModuleLicenses("$group:$name", version, found.getValue(coordinate))
    }
  }

  /** The POM of each module in [coordinates], read. */
  private fun poms(project: Project, coordinates: Set<String>): Map<String, PomLicenses> {
    val configuration =
      project.configurations.detachedConfiguration(
        *coordinates.map { project.dependencies.create("$it@pom") }.toTypedArray()
      )
    return configuration.incoming.artifacts.artifacts.associate { artifact ->
      val id = artifact.id.componentIdentifier as ModuleComponentIdentifier
      "${id.group}:${id.module}:${id.version}" to PomLicenses.read(artifact.file)
    }
  }
}
