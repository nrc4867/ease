package dev.chieppa.ease.codegen

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import dev.chieppa.ease.codegen.output.GpuWriterOutput
import dev.chieppa.ease.codegen.read.GpuStructReader

/**
 * Gives KSP the [GpuStructProcessor], wired with its reader and output; listed as a service so KSP
 * finds it.
 */
public class GpuStructProcessorProvider : SymbolProcessorProvider {
  override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
    GpuStructProcessor(
      reader = GpuStructReader(),
      outputs = listOf(GpuWriterOutput(environment.codeGenerator)),
      logger = environment.logger,
    )
}
