package dev.chieppa.ease.net.session

import dev.chieppa.ease.net.command.RateLimits
import dev.chieppa.ease.net.input.InputLayout
import dev.chieppa.ease.net.input.InputWindow

/** What host and clients of a session agree on: the input's shape and window, and rate limits. */
public class NetSettings(
  public val input: InputLayout = InputLayout(axes = 2, buttons = 8),
  public val window: InputWindow = InputWindow(),
  public val rateLimits: RateLimits = RateLimits(),
)
