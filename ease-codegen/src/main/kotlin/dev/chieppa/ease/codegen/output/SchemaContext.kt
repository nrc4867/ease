package dev.chieppa.ease.codegen.output

import dev.chieppa.ease.codegen.model.SerialType

/** What a JSON Schema is written against: its top-level type and every type reachable from it. */
internal class SchemaContext(val root: SerialType, val reachable: Map<String, SerialType>)
