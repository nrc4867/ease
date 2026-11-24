package dev.chieppa.ease.agent.tools

/** One part of a tool's answer. */
public sealed interface ToolPart {
  /** Text, usually JSON. */
  public class Text(public val text: String) : ToolPart

  /** A picture as PNG bytes. */
  public class Png(public val bytes: ByteArray) : ToolPart
}
