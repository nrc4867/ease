# {{projectName}}

A game on the Ease engine, pinned to Ease {{easeVersion}}. Ease is a composite build from `ease.dir`
in `gradle.properties`.

```
./gradlew check          # formatting, detekt, source rules and tests
./gradlew runGame        # the game in a window
./gradlew runHeadless    # 600 ticks with no window or GPU
```

The game's code is in `game/`. `ease generate component|system|module <Name>` adds to it.
