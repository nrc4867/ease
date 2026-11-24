package dev.chieppa.ease.codegen.output

import com.google.devtools.ksp.symbol.KSFile
import dev.chieppa.ease.codegen.model.ComponentModel

/**
 * One kind of file the processor writes from the models. The processor hands every output each
 * model that passed the rules, then calls [finish] once, so an output may write one file per
 * component or one file for them all.
 */
internal interface ModelOutput {
  /** Takes [model], read from [source]. */
  fun component(model: ComponentModel, source: KSFile)

  /** Called once every model of the round has been given. */
  fun finish() {}
}
