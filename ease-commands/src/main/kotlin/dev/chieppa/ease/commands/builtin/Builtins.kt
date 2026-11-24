package dev.chieppa.ease.commands.builtin

import dev.chieppa.ease.commands.CommandRegistry
import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.ecs.LocalTransformReplicated
import dev.chieppa.ease.ecs.replication.Authority

/** Registers the engine's own commands and components, and the components that replicate. */
internal fun CommandRegistry.registerBuiltins() {
  command(SetTransform::class, SetTransform.serializer())
  command(Spawn::class, Spawn.serializer())
  command(Destroy::class, Destroy.serializer())
  command(Restore::class, Restore.serializer())
  component(LocalTransform::class, LocalTransform.serializer())
  component(Authority::class, Authority.serializer())
  replicated(LocalTransformReplicated)
}
