package dev.chieppa.ease.codegen

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSNode
import dev.chieppa.ease.codegen.model.ComponentModel
import dev.chieppa.ease.codegen.output.ModelOutput
import dev.chieppa.ease.codegen.read.AnnotationNames
import dev.chieppa.ease.codegen.read.ComponentReader
import dev.chieppa.ease.codegen.rules.ModelRules

/**
 * Turns every `@Replicated` class into a [ComponentModel] with [reader], checks it against
 * [ModelRules], and hands each model that passes to every one of [outputs]. A class that breaks a
 * rule fails the build with an error on the class or its property, and nothing is written for it.
 */
internal class ReplicatedProcessor(
  private val reader: ComponentReader,
  private val outputs: List<ModelOutput<ComponentModel>>,
  private val logger: KSPLogger,
) : SymbolProcessor {
  override fun process(resolver: Resolver): List<KSAnnotated> {
    resolver.getSymbolsWithAnnotation(AnnotationNames.REPLICATED).forEach { symbol ->
      if (symbol is KSClassDeclaration) accept(symbol)
      else logger.error("@Replicated goes on a component class", symbol)
    }
    return emptyList()
  }

  override fun finish() {
    outputs.forEach { it.finish() }
  }

  private fun accept(declaration: KSClassDeclaration) {
    val model = reader.read(declaration)
    val problems = ModelRules.problems(model)
    problems.forEach { problem ->
      val node: KSNode =
        problem.field?.let { name ->
          declaration.declarations.firstOrNull { it.simpleName.asString() == name }
        } ?: declaration
      logger.error("${model.className}: ${problem.message}", node)
    }
    val source = declaration.containingFile
    if (problems.isNotEmpty() || source == null) return
    outputs.forEach { it.accept(model, source) }
  }
}
