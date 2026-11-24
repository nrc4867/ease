package dev.chieppa.ease.commands

private const val FNV_OFFSET = -0x340d631b7bdddcdbL
private const val FNV_PRIME = 0x100000001b3L
private const val SEPARATOR = '\n'

/**
 * A 64-bit FNV-1a hash of the serial names [registry] numbers on the wire: commands, components and
 * replicated types, each list sorted as the binary forms sort it and marked by its kind.
 */
internal fun typesHash(registry: CommandRegistry): Long {
  var hash = FNV_OFFSET
  fun add(text: String) {
    for (char in text) {
      hash = (hash xor char.code.toLong()) * FNV_PRIME
    }
    hash = (hash xor SEPARATOR.code.toLong()) * FNV_PRIME
  }
  add("commands")
  registry.commandTypes().map { it.second.descriptor.serialName }.sorted().forEach(::add)
  add("components")
  registry.componentTypes().map { it.second.descriptor.serialName }.sorted().forEach(::add)
  add("replicated")
  registry.replicatedSerializers().map { it.name }.sorted().forEach(::add)
  return hash
}
