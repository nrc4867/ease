package dev.chieppa.ease.assets.importing

import java.nio.file.Path

/** What one [ImportService.refresh] did. */
public class ImportReport(
  /** The sources imported again because their source, settings or importer changed. */
  public val imported: List<Path>,
  /** How many sources already had a result for their current inputs. */
  public val upToDate: Int,
  /** Every source that cannot import now; each keeps its last good result. */
  public val problems: List<ImportProblem>,
)
