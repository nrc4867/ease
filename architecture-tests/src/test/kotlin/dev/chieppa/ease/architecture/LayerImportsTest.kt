package dev.chieppa.ease.architecture

import com.lemonappdev.konsist.api.Konsist
import kotlin.test.Test
import kotlin.test.assertEquals

/** Code imports only from its own module and modules in lower layers. */
class LayerImportsTest {
  private val layers = ModuleLayers.fromSystemProperty()

  @Test
  fun `no module imports from its own layer or above`() {
    val violations =
      Konsist.scopeFromProject().files.flatMap { file ->
        val module = file.moduleName
        val layer = layers.layerOf(module) ?: return@flatMap emptyList()
        file.imports.mapNotNull { import ->
          val target = layers.moduleOfPackage(import.name) ?: return@mapNotNull null
          val targetLayer = layers.layerOf(target) ?: return@mapNotNull null
          val allowed = target == module || targetLayer < layer
          if (allowed) null else "${file.path}: imports ${import.name} from $target"
        }
      }

    assertEquals(emptyList(), violations)
  }
}
