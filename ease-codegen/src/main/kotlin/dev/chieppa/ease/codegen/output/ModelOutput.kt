package dev.chieppa.ease.codegen.output

import com.google.devtools.ksp.symbol.KSFile

/**
 * One kind of file the processor writes from models of type [M]. The processor hands every output
 * each model that passed the rules, then calls [finish] once, so an output may write one file per
 * model or one file for them all.
 */
internal interface ModelOutput<in M> {
  /** Takes [model], read from [source]. */
  fun accept(model: M, source: KSFile)

  /** Called once every model of the round has been given. */
  fun finish() {}
}
