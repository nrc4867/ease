package dev.chieppa.ease.assets.database

/** Thrown when the project's files are not in a state the database loads; names every problem. */
public class AssetDatabaseRejected(public val problems: List<AssetProblem>) :
  RuntimeException(problems.joinToString("\n") { it.message })
