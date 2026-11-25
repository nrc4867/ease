package dev.chieppa.ease.net.session

import dev.chieppa.ease.net.command.RateLimits
import dev.chieppa.ease.net.input.InputLayout
import dev.chieppa.ease.net.input.InputWindow
import dev.chieppa.ease.net.join.ContentManifest

/**
 * What host and clients of a session agree on: the input's shape and window, rate limits, and the
 * [content] each builds its worlds from, which the join handshake compares.
 */
public class NetSettings(
  public val input: InputLayout = InputLayout(axes = 2, buttons = 8),
  public val window: InputWindow = InputWindow(),
  public val rateLimits: RateLimits = RateLimits(),
  public val content: ContentManifest = ContentManifest.EMPTY,
)
