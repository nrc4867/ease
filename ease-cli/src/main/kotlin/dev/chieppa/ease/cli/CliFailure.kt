package dev.chieppa.ease.cli

/** Thrown for a mistake the person running a command can fix; the message says how. */
public class CliFailure(message: String, cause: Throwable? = null) :
  RuntimeException(message, cause)
