package dev.chieppa.ease.commands

/** A command could not apply to the world it was given; the world is unchanged. */
public class CommandRejected(message: String) : RuntimeException(message)
