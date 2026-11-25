package dev.chieppa.ease.cli

/** The statuses a command ends with. */
public object ExitCodes {
  /** The command did what it was asked. */
  public const val OK: Int = 0

  /** The command could not do what it was asked; it printed why. */
  public const val FAILED: Int = 1

  /** The arguments were not understood; the command printed its usage. */
  public const val USAGE: Int = 2
}
