package dev.chieppa.ease.codegen.model

/**
 * A type declared in the module being processed, with every type reachable from it, by serial name,
 * so its output can spell out what it refers to.
 */
internal data class DescribedType(val type: SerialType, val reachable: Map<String, SerialType>)
