package dev.chieppa.ease.agent.tools

/** What a tool call gives: its [parts], and whether it is an [error] the client's model reads. */
public class ToolAnswer(public val parts: List<ToolPart>, public val error: Boolean = false) {
  public companion object {
    /** An answer of [text] alone. */
    public fun text(text: String): ToolAnswer = ToolAnswer(listOf(ToolPart.Text(text)))

    /** An error answer explaining itself in [text]. */
    public fun error(text: String): ToolAnswer =
      ToolAnswer(listOf(ToolPart.Text(text)), error = true)
  }
}
