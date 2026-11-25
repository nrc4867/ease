package dev.chieppa.ease.project.merge

import dev.chieppa.ease.project.git.Git
import dev.chieppa.ease.serialization.merge.TreeMerge
import dev.chieppa.ease.serialization.yaml.CanonicalYaml
import dev.chieppa.ease.serialization.yaml.YamlFormatException
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.serialization.json.JsonElement

/**
 * Ease's git merge driver for scene, prefab and node files, which are canonical YAML. It merges the
 * three versions key by key (see [TreeMerge]), so two branches that add or change neighbouring keys
 * merge where git's line merge would stop. When both sides changed one value, or a file does not
 * read as YAML, it leaves git's ordinary conflict markers in the file for a person to settle; it
 * never picks a side.
 */
public class MergeDriver(private val git: Git) {
  /**
   * Merges [other] into [current], both edited from [ancestor] (an empty file when both added the
   * file), writing the result into [current]. Returns whether the merge is clean.
   */
  public fun merge(ancestor: Path, current: Path, other: Path): Boolean {
    val merged = treeMerge(ancestor, current, other)
    if (merged != null) {
      Files.writeString(current, CanonicalYaml.write(merged))
      return true
    }
    return git
      .run(
        "merge-file",
        "-L",
        "ours",
        "-L",
        "base",
        "-L",
        "theirs",
        "$current",
        "$ancestor",
        "$other",
      )
      .succeeded
  }

  /** The merged tree, or null when the sides disagree or a file is not YAML. */
  private fun treeMerge(ancestor: Path, current: Path, other: Path): JsonElement? =
    try {
      val outcome = TreeMerge.merge(parse(ancestor), parse(current), parse(other))
      outcome.tree.takeIf { outcome.clean }
    } catch (_: YamlFormatException) {
      null
    }

  private fun parse(file: Path): JsonElement? {
    val text = Files.readString(file)
    return if (text.isBlank()) null else CanonicalYaml.read(text)
  }
}
