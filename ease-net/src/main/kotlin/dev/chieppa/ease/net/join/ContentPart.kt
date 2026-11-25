package dev.chieppa.ease.net.join

/**
 * One piece of content a [ContentManifest] covers beyond the shipped assets, such as an enabled
 * mod: its [id] and the [hash] of what it holds.
 */
public class ContentPart(public val id: String, public val hash: String)
