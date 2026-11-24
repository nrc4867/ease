package dev.chieppa.ease.annotations

/**
 * The documentation of a public serialized type or field: one line describing what it does now.
 * ease-codegen writes it into `ease.d.ts` as a doc comment and into the JSON Schemas as
 * `description`. It stands in for KDoc on those declarations, so the text exists once.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
public annotation class Doc(val text: String)
