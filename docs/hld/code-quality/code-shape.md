# Code shape

**No algorithm on one line.** Each step of an algorithm gets its own named value or function, so it can be read, stepped through in a debugger and profiled. The 100-column limit and detekt's chained-call and nesting rules catch most violations; the rest is review.

**Files around 300 lines.** Past 300 the source check warns; past 400 it fails. Crossing the line is a prompt to split by responsibility, not to compress code.

**No mega classes.** A feature should add files, not edit a central one. Concretely:

- Features register themselves (components, systems, commands, importers, render passes, conditions) through their own module's contribution, found by `ServiceLoader` or listed in that module. There is no central `when` over every feature and no registry file that every feature edits.
- Architecture tests (Konsist) fail when a foundation or core module imports a feature module, or when a class depends on more than a set number of feature packages.
- A useful signal in review: a pull request that adds a feature and also edits a file outside its module needs a reason.

**Dependency injection: no framework.** Constructor injection everywhere, wired by hand in each host's composition root (the player, the editor, Squash). Reasons: runtime DI frameworks use reflection, slow startup and hide wiring, which hurts performance and trimmed builds; hand wiring in three small roots stays readable. If wiring outgrows that, the only acceptable step up is compile-time injection generated with KSP (such as kotlin-inject), never a runtime container.
