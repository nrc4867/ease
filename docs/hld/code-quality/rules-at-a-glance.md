# Code quality: rules at a glance

Every rule here is checked by a tool, not by memory or review. Rules start from Squash's conventions; these pages list what is enforced and how. A rule nothing can check is a guideline and is marked as one.

| Rule | Enforced by |
|---|---|
| Google style formatting, Kotlin and Java | Spotless (ktfmt Google style, google-java-format), pre-commit hook and CI |
| No algorithm crammed onto one line | 100-column limit; detekt limits on chained calls per line and nesting |
| Files around 300 lines | Source check: warns over 300, fails over 400 |
| Single responsibility, small classes | detekt size and complexity rules; guideline in review |
| No mega classes edited by every feature | Architecture tests (Konsist); registration through contributions |
| Module layers | Gradle dependencies: a module declares only modules below it |
| No waits, sleeps or polling | Source check on main and test code |
| Async tested with async tools | `kotlinx-coroutines-test` and injected dispatchers and clocks |
| Test public behavior: inputs and outcomes | Guideline, backed by the no-reflection check |
| No reflection to widen visibility in tests | Source check on test code |
| No golden files or reference images | Source check: no committed expected-output files, no update-goldens flag |
| Performance budgets | Allocation-count tests in CI; benchmarks tracked, not gated |
| Remove, never deprecate | detekt forbids `@Deprecated` |
| Comments describe current behavior only | Source check for history words; guideline for length |
| Small Markdown files | Source check: fails a `.md` file over 150 lines |
| Shipped dependencies use permissive licenses | `checkLicenses` and `checkNotice` in `check`; see `dependencies.md` |
| Tests run on GitHub's free runners | CI on `ubuntu-latest`, GPU tests on software Vulkan |
