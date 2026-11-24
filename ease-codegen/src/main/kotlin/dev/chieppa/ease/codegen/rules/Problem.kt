package dev.chieppa.ease.codegen.rules

/** One reason a model cannot replicate, on [field], or on the class when it is null. */
internal data class Problem(val field: String?, val message: String)
