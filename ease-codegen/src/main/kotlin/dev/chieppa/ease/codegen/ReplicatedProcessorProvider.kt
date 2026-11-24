package dev.chieppa.ease.codegen

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import dev.chieppa.ease.codegen.output.SerializerOutput
import dev.chieppa.ease.codegen.read.ComponentReader
import dev.chieppa.ease.codegen.read.replicationOptionReaders

/**
 * Gives KSP the [ReplicatedProcessor], wired with its readers and outputs; listed as a service so
 * KSP finds it.
 */
public class ReplicatedProcessorProvider : SymbolProcessorProvider {
  override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
    ReplicatedProcessor(
      reader = ComponentReader(replicationOptionReaders),
      outputs = listOf(SerializerOutput(environment.codeGenerator)),
      logger = environment.logger,
    )
}
