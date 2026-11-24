package dev.chieppa.ease.agent.tools

/** A tool will not do what it was asked, for the reason in [message], which the model reads. */
public class ToolRefusal(message: String) : Exception(message)
