package dev.chieppa.ease.codegen

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

/** Gives KSP the [ReplicatedProcessor]; listed as a service so KSP finds it. */
public class ReplicatedProcessorProvider : SymbolProcessorProvider {
  override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
    ReplicatedProcessor(environment.codeGenerator, environment.logger)
}
