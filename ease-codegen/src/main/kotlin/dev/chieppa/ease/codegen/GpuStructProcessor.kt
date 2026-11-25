package dev.chieppa.ease.codegen

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import dev.chieppa.ease.codegen.model.GpuStructLayout
import dev.chieppa.ease.codegen.output.ModelOutput
import dev.chieppa.ease.codegen.read.AnnotationNames
import dev.chieppa.ease.codegen.read.GpuStructReader
import dev.chieppa.ease.codegen.rules.GpuStructRules

/**
 * Turns every `@GpuStruct` class into a [GpuStructLayout] with [reader], checks it against
 * [GpuStructRules], and hands each layout that passes to every one of [outputs]. A class that
 * breaks a rule fails the build with an error on it, and nothing is written for it.
 */
internal class GpuStructProcessor(
  private val reader: GpuStructReader,
  private val outputs: List<ModelOutput<GpuStructLayout>>,
  private val logger: KSPLogger,
) : SymbolProcessor {
  override fun process(resolver: Resolver): List<KSAnnotated> {
    resolver.getSymbolsWithAnnotation(AnnotationNames.GPU_STRUCT).forEach { symbol ->
      if (symbol is KSClassDeclaration) accept(symbol)
      else logger.error("@GpuStruct goes on a class", symbol)
    }
    return emptyList()
  }

  private fun accept(declaration: KSClassDeclaration) {
    val model = reader.read(declaration)
    val problems = GpuStructRules.problems(model)
    problems.forEach { problem ->
      val field = problem.field?.let { "field $it: " }.orEmpty()
      logger.error("${model.className}: $field${problem.message}", declaration)
    }
    val source = declaration.containingFile
    if (problems.isNotEmpty() || source == null) return
    val layout = GpuStructLayout.of(model)
    outputs.forEach { it.accept(layout, source) }
  }
}
