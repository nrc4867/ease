# Formatting and static analysis

- **Formatting:** Spotless runs ktfmt in Google style for Kotlin and google-java-format for Java. Nobody argues about formatting; `spotlessApply` fixes it.
- **Static analysis:** detekt with a checked-in config. Rules that matter most here: line length 100, chained calls per line, nesting depth, method length, cyclomatic complexity, class size, forbidden annotations (`@Deprecated`) and forbidden calls.
- **Source checks:** one Gradle task, `checkSources`, for rules no linter covers: file and Markdown length, waits and sleeps, reflection in tests, history words in comments, golden files. It is plain Kotlin with a test for each rule.
- **Pre-commit hook:** a Gradle task installs a git hook on first build. The hook runs `spotlessCheck` and `checkSources` on staged files only, so it takes seconds. CI runs the same checks on everything, so skipping the hook gains nothing.
