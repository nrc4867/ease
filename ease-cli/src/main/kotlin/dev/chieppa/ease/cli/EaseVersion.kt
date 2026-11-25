package dev.chieppa.ease.cli

import java.util.Properties

/** The version of Ease this command line was built from. */
public object EaseVersion {
  private const val RESOURCE = "ease-cli.properties"
  private const val KEY = "easeVersion"

  /** The version a new game pins by default. */
  public val current: String by lazy {
    val stream =
      checkNotNull(EaseVersion::class.java.classLoader.getResourceAsStream(RESOURCE)) {
        "No $RESOURCE in the distribution"
      }
    val properties = Properties().apply { stream.use { load(it) } }
    checkNotNull(properties.getProperty(KEY)) { "$RESOURCE has no $KEY" }
  }
}
