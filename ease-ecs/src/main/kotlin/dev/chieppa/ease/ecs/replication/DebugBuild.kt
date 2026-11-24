package dev.chieppa.ease.ecs.replication

/**
 * Whether this is a debug build: JVM assertions are on for the engine, as they are in every test
 * run and every development launch, and off in a released game.
 */
public object DebugBuild {
  /** True in a debug build. */
  public val enabled: Boolean = DebugBuild::class.java.desiredAssertionStatus()
}
