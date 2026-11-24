package dev.chieppa.ease.ecs.replication

/** What reading received bytes came to: [OK], or the first reason they were refused. */
public enum class DecodeStatus {
  /** Every byte was read and accepted. */
  OK,

  /** The packet is longer than any packet the engine sends. */
  TOO_LONG,

  /** The bytes end before what they announce. */
  TRUNCATED,

  /** A variable-length number runs past its longest form. */
  MALFORMED,

  /** More entities than a packet may carry. */
  TOO_MANY_ENTITIES,

  /** More components on one entity than there are replicated types. */
  TOO_MANY_COMPONENTS,

  /** A network id that no entity can have. */
  BAD_NETWORK_ID,

  /** A network id no live entity carries. */
  UNKNOWN_ENTITY,

  /** State for an entity this world holds authority over, which only this world may change. */
  LOCAL_AUTHORITY,

  /** A component type id outside the registered replicated types. */
  UNKNOWN_TYPE,

  /** State for a component the entity does not carry. */
  MISSING_COMPONENT,

  /** A value no field may hold, such as a float that is not finite. */
  BAD_VALUE,

  /** Bytes left over after the last entity. */
  TRAILING_DATA,
}
