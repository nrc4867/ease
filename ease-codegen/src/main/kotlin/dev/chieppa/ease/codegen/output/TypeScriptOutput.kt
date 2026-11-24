package dev.chieppa.ease.codegen.output

import com.google.devtools.ksp.symbol.KSFile
import dev.chieppa.ease.codegen.model.DescribedType

/**
 * Writes the module's part of `ease.d.ts`: the serializable types it declares and what they reach.
 */
internal class TypeScriptOutput(private val writer: ResourceWriter) : ModelOutput<DescribedType> {
  private val types = mutableListOf<DescribedType>()
  private val sources = mutableSetOf<KSFile>()

  override fun accept(model: DescribedType, source: KSFile) {
    types += model
    sources += source
  }

  override fun finish() {
    if (types.isEmpty()) return
    val content = TypeScriptSource.of(types)
    writer.write(sources, ResourceFiles.TYPES, ResourceFiles.DECLARATIONS, content)
  }
}
