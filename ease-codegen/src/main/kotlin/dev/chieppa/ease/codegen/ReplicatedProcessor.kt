package dev.chieppa.ease.codegen

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration

/** Writes an object beside every class annotated `@Replicated`, naming the class. */
public class ReplicatedProcessor(
  private val generator: CodeGenerator,
  private val logger: KSPLogger,
) : SymbolProcessor {
  override fun process(resolver: Resolver): List<KSAnnotated> {
    val annotated = resolver.getSymbolsWithAnnotation(REPLICATED).toList()
    annotated.forEach { symbol ->
      if (symbol !is KSClassDeclaration) {
        logger.error("@Replicated goes on a component class", symbol)
        return@forEach
      }
      write(symbol)
    }
    return emptyList()
  }

  private fun write(declaration: KSClassDeclaration) {
    val packageName = declaration.packageName.asString()
    val name = declaration.simpleName.asString() + SUFFIX
    val source = checkNotNull(declaration.containingFile) { "$name has no source file" }
    val output = generator.createNewFile(Dependencies(false, source), packageName, name)
    val qualified = declaration.qualifiedName?.asString().orEmpty()
    val text =
      "package $packageName\n\n" +
        "public object $name {\n" +
        "  public const val NAME: String = \"$qualified\"\n" +
        "}\n"
    output.bufferedWriter().use { it.write(text) }
  }

  private companion object {
    const val REPLICATED = "dev.chieppa.ease.ecs.replication.Replicated"
    const val SUFFIX = "Replicated"
  }
}
