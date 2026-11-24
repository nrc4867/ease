package dev.chieppa.ease.commands.builtin

import dev.chieppa.ease.commands.CommandRegistry
import dev.chieppa.ease.ecs.LocalTransform

/** Registers the engine's own commands and components. */
internal fun CommandRegistry.registerBuiltins() {
  command(SetTransform::class, SetTransform.serializer())
  command(Spawn::class, Spawn.serializer())
  command(Destroy::class, Destroy.serializer())
  command(Restore::class, Restore.serializer())
  component(LocalTransform::class, LocalTransform.serializer())
}
