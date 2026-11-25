# {{projectName}}

A game on the Ease engine, pinned to Ease {{easeVersion}}. Ease is a composite build from `ease.dir`
in `gradle.properties`.

```
./gradlew check          # formatting, detekt, source rules and tests
./gradlew runGame        # the game in a window
./gradlew runHeadless    # 600 ticks with no window or GPU
```

The game's code is in `game/`. `ease generate component|system|module <Name>` adds to it.

## Version control

`ease new` made this folder a git repository: `.gitignore` for build output and per-user state,
`.gitattributes` with binary assets in Git LFS (lockable) and Ease's merge driver for scenes and
prefabs, and a pre-commit hook that runs `ease check --staged` (what is about to be committed). The merge driver and the hook are in this
clone's `.git` folder, so another clone registers the driver itself with
`git config merge.ease.driver "ease merge-driver %O %A %B %P"` and installs Git LFS.

```
ease check          # sidecars, GUIDs, references, nodes, conflict markers, LFS coverage
ease check --staged # the same rules over git's index
ease check --fix    # the safe repairs: respace order keys, settle an add/add GUID, restore deleted assets
```

Run `ease check` in CI as a step of the job that builds the game; it ends with status 1 when it finds
an error, and needs only git (and Git LFS for the coverage warning) beside the `ease` command.
