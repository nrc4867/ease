# Versioning

Ease is versioned in two places: the engine's releases, which game repos depend on, and the files and protocols that outlive a single build.

## Releases

- **Semantic versions,** tagged `vX.Y.Z`. While the API settles, Ease stays on `0.x`, and a minor release may break the API.
- **Migration notes** for game repos go in the GitHub release notes, under a "Migrating" heading, not in Markdown in the repo. Git history is the only changelog.
- **Game projects pin an exact Ease version** in their project file, never a range, so a game changes engines only when it chooses to.

## Inside the repo

"Remove, never deprecate" applies: an API that changes has every caller changed in the same pull request.

## Versioned surfaces outside the repo

These outlive a build, so each carries an integer `formatVersion`:

- project, scene, prefab and asset files;
- saves;
- the plugin API;
- the network protocol.

A newer build reads every older version through migrations written as code, one step at a time: a migration takes version N to N+1, and reading a file runs each step from its version up to the current one.

## Tests

Each migration step has a test that builds a document at the older version in code, migrates it, and checks the result field by field. No files of older versions are committed as fixtures, and no golden files.
