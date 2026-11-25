package dev.chieppa.ease.cli.template

/** A template or generator: its manifest, and its files with the shared parts it includes. */
public class Template(
  /** The folder name that selects it. */
  public val id: String,
  /** The manifest. */
  public val manifest: TemplateManifest,
  /** The parts' files first, then the template's own. */
  public val files: List<TemplateFile>,
)
