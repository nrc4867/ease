package dev.chieppa.ease.codegen.output

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSFile

/** Writes one resource file under the module's `META-INF/ease`, rebuilt when any source changes. */
internal class ResourceWriter(private val generator: CodeGenerator) {
  fun write(sources: Collection<KSFile>, name: String, extension: String, content: String) {
    val dependencies = Dependencies(true, *sources.toTypedArray())
    val file = generator.createNewFile(dependencies, ResourceFiles.DIRECTORY, name, extension)
    file.bufferedWriter().use { it.write(content) }
  }
}
