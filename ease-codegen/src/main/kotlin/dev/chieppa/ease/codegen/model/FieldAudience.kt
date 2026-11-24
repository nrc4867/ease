package dev.chieppa.ease.codegen.model

/** Who receives a field, as the `Audience` enum of `ease-ecs` names it. */
internal enum class FieldAudience {
  EVERYONE,
  OWNER_ONLY,
  ALL_BUT_OWNER,
}
