package dev.chieppa.ease.ecs.replication

/**
 * The limits of a state packet: a count of entities, then for each its network id, a count of
 * components, and for each a type id and the serializer's delta. A reader refuses anything past
 * them before reading further.
 */
public object StatePackets {
  /** The longest packet, in bytes: what fits one datagram on every common path. */
  public const val MAX_BYTES: Int = 1200

  /** The most entities one packet carries. */
  public const val MAX_ENTITIES: Int = 1000

  /** Bits the entity count takes. */
  public const val ENTITY_COUNT_BITS: Int = 10

  /** Bits each entity's component count takes. */
  public const val COMPONENT_COUNT_BITS: Int = 9
}
