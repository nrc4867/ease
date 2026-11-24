package dev.chieppa.ease.ecs.replication

/** A [BitWriter] had no room for what it was asked to write. */
public class BitWriterFull(message: String) : IllegalStateException(message)
