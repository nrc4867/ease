package dev.chieppa.ease.codegen.output

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSFile
import dev.chieppa.ease.codegen.model.GpuStructLayout

/** Writes each GPU struct's writer beside its class, rebuilt whenever that class's file changes. */
internal class GpuWriterOutput(private val generator: CodeGenerator) :
  ModelOutput<GpuStructLayout> {
  override fun accept(model: GpuStructLayout, source: KSFile) {
    val dependencies = Dependencies(false, source)
    val struct = model.model
    val file = generator.createNewFile(dependencies, struct.packageName, struct.writerName)
    file.bufferedWriter().use { it.write(GpuWriterSource.of(model)) }
  }
}
