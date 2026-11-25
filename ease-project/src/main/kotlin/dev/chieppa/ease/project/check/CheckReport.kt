package dev.chieppa.ease.project.check

/** Everything [ProjectChecker] found, errors first, each group in file order. */
public class CheckReport(findings: List<Finding>) {
  /** Every finding. */
  public val findings: List<Finding> =
    findings.sortedWith(compareBy({ it.severity }, { it.file }, { it.kind }))

  /** The findings that stop a commit. */
  public val errors: List<Finding>
    get() = findings.filter { it.severity == Severity.ERROR }

  /** The findings that do not. */
  public val warnings: List<Finding>
    get() = findings.filter { it.severity == Severity.WARNING }

  /** Whether the project has no errors. */
  public val passed: Boolean
    get() = errors.isEmpty()

  /** The one-line count that ends a report, such as `2 errors, 1 warning`. */
  public fun summary(): String =
    if (findings.isEmpty()) "no problems"
    else "${count(errors.size, "error")}, ${count(warnings.size, "warning")}"

  private fun count(number: Int, noun: String): String =
    "$number $noun" + if (number == 1) "" else "s"
}
