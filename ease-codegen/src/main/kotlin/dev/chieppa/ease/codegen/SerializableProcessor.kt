package dev.chieppa.ease.codegen

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.Origin
import dev.chieppa.ease.codegen.model.DescribedType
import dev.chieppa.ease.codegen.model.SerialType
import dev.chieppa.ease.codegen.output.ModelOutput
import dev.chieppa.ease.codegen.read.AnnotationNames
import dev.chieppa.ease.codegen.read.SerialTypeReader
import dev.chieppa.ease.codegen.read.serialName
import dev.chieppa.ease.codegen.rules.SchemaRules

/**
 * Describes every `@Serializable` class the module declares with [reader], checks it against
 * [SchemaRules], and hands each type that passes, with everything reachable from it, to every one
 * of [outputs]. A class with a meaningless `@Range` fails the build with an error on its property,
 * and nothing is written for it.
 */
internal class SerializableProcessor(
  private val reader: SerialTypeReader,
  private val outputs: List<ModelOutput<DescribedType>>,
  private val logger: KSPLogger,
) : SymbolProcessor {
  override fun process(resolver: Resolver): List<KSAnnotated> {
    resolver
      .getSymbolsWithAnnotation(AnnotationNames.SERIALIZABLE)
      .filterIsInstance<KSClassDeclaration>()
      .filter { it.origin == Origin.KOTLIN && it.containingFile != null }
      .forEach { accept(it) }
    return emptyList()
  }

  override fun finish() {
    outputs.forEach { it.finish() }
  }

  /** The problems of [type], each with the message and the node to report it on. */
  private fun problemsOf(type: SerialType, declaration: KSClassDeclaration) =
    SchemaRules.problems(type).map { problem ->
      val own = type.serialName == declaration.serialName()
      val node: KSNode =
        problem.field
          ?.takeIf { own }
          ?.let { name ->
            declaration.declarations.firstOrNull { it.simpleName.asString() == name }
          } ?: declaration
      "${type.serialName}: ${problem.message}" to node
    }

  private fun accept(declaration: KSClassDeclaration) {
    val described = reader.describe(declaration)
    val problems = described.reachable.values.flatMap { type -> problemsOf(type, declaration) }
    problems.forEach { (message, node) -> logger.error(message, node) }
    val source = declaration.containingFile
    if (problems.isNotEmpty() || source == null) return
    outputs.forEach { it.accept(described, source) }
  }
}
