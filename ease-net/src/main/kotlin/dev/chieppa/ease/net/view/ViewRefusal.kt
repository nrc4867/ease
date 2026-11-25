package dev.chieppa.ease.net.view

import dev.chieppa.ease.ecs.replication.DecodeStatus

/**
 * Why a client refused a spawn or despawn, which closes its connection. A refusal that
 * [contentMismatch]es means the client's files differ from the host's, so it cannot build what the
 * host names; any other is a malformed or impossible message.
 */
public enum class ViewRefusal(public val contentMismatch: Boolean = false) {
  /** The message is longer than any the host sends. */
  TOO_LONG,

  /** The message ends before what it announces. */
  TRUNCATED,

  /** A variable-length number runs past its longest form. */
  MALFORMED,

  /** A message kind other than spawn or despawn. */
  UNKNOWN_KIND,

  /** A network id no entity can have. */
  BAD_NETWORK_ID,

  /** A spawn for a network id a live entity already carries. */
  DUPLICATE_ENTITY,

  /** A despawn for a network id no live entity carries. */
  UNKNOWN_ENTITY,

  /** A despawn for an entity this world holds, which only this world may remove. */
  LOCAL_AUTHORITY,

  /** An owner that no peer can be. */
  BAD_PEER,

  /** A spawn form other than plain or scene node. */
  UNKNOWN_FORM,

  /** A node key or placement depth out of bounds. */
  BAD_KEY,

  /** A component type id outside the registered replicated types. */
  UNKNOWN_TYPE,

  /** More components than there are replicated types. */
  TOO_MANY_COMPONENTS,

  /** The same component type twice. */
  DUPLICATE_COMPONENT,

  /** A value no field may hold, such as a float that is not finite. */
  BAD_VALUE,

  /** Bytes left over after the message. */
  TRAILING_DATA,

  /** A scene node whose key a live node of this world already has. */
  NODE_EXISTS,

  /** A scene node, in a world with no scene nodes system. */
  NO_SCENES(contentMismatch = true),

  /** A scene node of an asset this peer's files do not have. */
  UNKNOWN_ASSET(contentMismatch = true),

  /** A scene node its asset, resolved from this peer's files, does not have. */
  UNKNOWN_NODE(contentMismatch = true);

  public companion object {
    /** The refusal for a bit reader's [status], which is not `OK`. */
    public fun of(status: DecodeStatus): ViewRefusal =
      when (status) {
        DecodeStatus.TRUNCATED -> TRUNCATED
        DecodeStatus.BAD_VALUE -> BAD_VALUE
        else -> MALFORMED
      }
  }
}
