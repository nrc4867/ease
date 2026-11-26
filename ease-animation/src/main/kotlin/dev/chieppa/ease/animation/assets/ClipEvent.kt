package dev.chieppa.ease.animation.assets

/**
 * A named moment of a clip, such as a foot coming down: [name] fires when playback passes [time],
 * in seconds from the clip's start.
 */
public class ClipEvent(public val name: String, public val time: Float)
