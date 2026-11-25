package dev.chieppa.ease.project.git

/** Thrown when git cannot be run or a command that had to succeed did not; the message says why. */
public class GitFailure(message: String, cause: Throwable? = null) :
  RuntimeException(message, cause)
