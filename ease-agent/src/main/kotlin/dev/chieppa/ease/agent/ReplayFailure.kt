package dev.chieppa.ease.agent

/** A world could not be reproduced from another's command log, for the reason in [message]. */
public class ReplayFailure(message: String) : Exception(message)
