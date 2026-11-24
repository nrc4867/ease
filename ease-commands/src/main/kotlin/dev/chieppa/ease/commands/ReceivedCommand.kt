package dev.chieppa.ease.commands

/** What reading a command received from a peer came to: the command, or why it was refused. */
public sealed interface ReceivedCommand {
  /** The bytes held a registered command. */
  public class Accepted(public val command: Command) : ReceivedCommand

  /**
   * The bytes were refused for [reason], with the decoder's [detail] for logs; the connection that
   * sent them closes.
   */
  public class Refused(public val reason: CommandRefusal, public val detail: String?) :
    ReceivedCommand
}

/** Why received command bytes were refused. */
public enum class CommandRefusal {
  /** Longer than any command a peer may send. */
  TOO_LONG,

  /** Not a registered command in the binary form: unknown type, wrong shape or cut short. */
  MALFORMED,
}
