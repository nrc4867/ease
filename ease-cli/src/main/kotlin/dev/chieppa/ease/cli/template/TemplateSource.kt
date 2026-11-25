package dev.chieppa.ease.cli.template

import java.net.URI
import java.nio.file.FileSystems
import java.nio.file.Path

/**
 * Where templates come from: project templates (`ease new`), generators (`ease generate`) and the
 * shared parts both include. Each is a folder: a manifest and a `files` folder.
 */
public interface TemplateSource {
  /** The ids of the project templates, sorted. */
  public fun projects(): List<String>

  /** The ids of the generators, sorted. */
  public fun generators(): List<String>

  /** The project template [id], with its parts. Fails, naming the ids there are, when unknown. */
  public fun project(id: String): Template

  /** The generator [id], with its parts. Fails, naming the ids there are, when unknown. */
  public fun generator(id: String): Template

  /** Where a source is found. */
  public companion object {
    private const val RESOURCE_ROOT = "ease-templates"

    /** The templates shipped inside the distribution. */
    public fun shipped(
      loader: ClassLoader = checkNotNull(TemplateSource::class.java.classLoader)
    ): TemplateSource {
      val url =
        checkNotNull(loader.getResource(RESOURCE_ROOT)) { "No $RESOURCE_ROOT in the distribution" }
      val uri = url.toURI()
      if (uri.scheme == "jar") openArchive(uri)
      return DirectoryTemplates(Path.of(uri))
    }

    /** The templates in [root], laid out as the shipped ones are. */
    public fun directory(root: Path): TemplateSource = DirectoryTemplates(root)

    private fun openArchive(uri: URI) {
      val archive = URI.create(uri.toString().substringBefore("!"))
      try {
        FileSystems.getFileSystem(archive)
      } catch (_: java.nio.file.FileSystemNotFoundException) {
        FileSystems.newFileSystem(archive, emptyMap<String, Any>())
      }
    }
  }
}
