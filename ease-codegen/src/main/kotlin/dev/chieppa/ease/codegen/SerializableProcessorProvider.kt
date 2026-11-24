package dev.chieppa.ease.codegen

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import dev.chieppa.ease.codegen.output.JsonSchemaOutput
import dev.chieppa.ease.codegen.output.ResourceWriter
import dev.chieppa.ease.codegen.output.TypeScriptOutput
import dev.chieppa.ease.codegen.read.SerialTypeReader

/**
 * Gives KSP the [SerializableProcessor], wired with its reader and outputs; listed as a service so
 * KSP finds it.
 */
public class SerializableProcessorProvider : SymbolProcessorProvider {
  override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
    val writer = ResourceWriter(environment.codeGenerator)
    return SerializableProcessor(
      reader = SerialTypeReader(),
      outputs = listOf(JsonSchemaOutput(writer), TypeScriptOutput(writer)),
      logger = environment.logger,
    )
  }
}
