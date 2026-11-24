package dev.chieppa.ease.ecs.replication

/**
 * How a world takes part in replication: the peer it runs as, the component types that replicate,
 * and whether writes are guarded as in a debug build.
 */
public class ReplicationSettings(
  public val localPeer: PeerId = PeerId.HOST,
  public val serializers: List<ReplicatedSerializer<*>> = emptyList(),
  public val debugChecks: Boolean = DebugBuild.enabled,
)
