package dev.chieppa.ease.codegen.output

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSFile
import dev.chieppa.ease.codegen.model.ComponentModel

/** Writes each model's serializer beside its class, rebuilt whenever that class's file changes. */
internal class SerializerOutput(private val generator: CodeGenerator) :
  ModelOutput<ComponentModel> {
  override fun accept(model: ComponentModel, source: KSFile) {
    val dependencies = Dependencies(false, source)
    val file = generator.createNewFile(dependencies, model.packageName, model.serializerName)
    file.bufferedWriter().use { it.write(SerializerSource.of(model)) }
  }
}
