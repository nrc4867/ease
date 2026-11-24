package dev.chieppa.ease.architecture

/** One module of the build: its layer's rank (lowest is 0) and the package its code lives in. */
data class LayeredModule(val name: String, val rank: Int, val packageName: String)

/**
 * The modules and their layers, as the build passes them in the `ease.layers` system property
 * (`module:rank:package,...`).
 */
class ModuleLayers(private val modules: List<LayeredModule>) {
  private val byName = modules.associateBy { it.name }

  /** The rank of [module]'s layer, or null for a module outside the layers. */
  fun layerOf(module: String): Int? = byName[module]?.rank

  /** The module whose package [qualifiedName] is in, or null when it is in none. */
  fun moduleOfPackage(qualifiedName: String): String? =
    modules
      .filter { qualifiedName.startsWith(it.packageName + ".") }
      .maxByOrNull { it.packageName.length }
      ?.name

  companion object {
    fun fromSystemProperty(): ModuleLayers {
      val property =
        System.getProperty("ease.layers") ?: error("ease.layers is set by the Gradle build")
      val modules =
        property.split(',').map { triple ->
          val (name, rank, packageName) = triple.split(':')
          LayeredModule(name, rank.toInt(), packageName)
        }
      return ModuleLayers(modules)
    }
  }
}
