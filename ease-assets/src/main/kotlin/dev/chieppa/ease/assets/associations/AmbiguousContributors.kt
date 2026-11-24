package dev.chieppa.ease.assets.associations

/**
 * Thrown when a file that takes one contributor of a [kind] has two that tie. The message names the
 * file and where each contender came from; a tie is never broken silently.
 */
public class AmbiguousContributors(
  public val file: ProjectFile,
  public val kind: ContributorKind<*>,
  public val contenders: List<Attachment>,
) :
  RuntimeException(
    "$file has ${contenders.size} equally specific ${kind.name} contributors: " +
      contenders.joinToString { "${it.origin} (${it.match}, priority ${it.priority})" }
  )
