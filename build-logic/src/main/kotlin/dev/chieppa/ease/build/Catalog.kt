package dev.chieppa.ease.build

import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.getByType

/** The repository's version catalog, `gradle/libs.versions.toml`, for convention plugins. */
val Project.catalog: VersionCatalog
  get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

/** The version named [alias] in the catalog. */
fun Project.catalogVersion(alias: String): String = catalog.findVersion(alias).get().requiredVersion

/** The library named [alias] in the catalog. */
fun Project.catalogLibrary(alias: String): Provider<MinimalExternalModuleDependency> =
  catalog.findLibrary(alias).get()
