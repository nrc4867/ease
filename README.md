# Ease

A game engine and editor for the JVM. The engine is a set of libraries in Kotlin and Java that render through WebGPU (wgpu-native over the Foreign Function & Memory API); the editor is a [Stretch](https://github.com/nrc4867/stretch) app that opens game projects; any game runs in the editor, as a standalone player, or headless.

The design lives in [`docs/hld/`](docs/hld/README.md).

## Build

Needs JDK 25 and a checkout of [Stretch](https://github.com/nrc4867/stretch) beside this one (or `stretch.dir=<path>` in `~/.gradle/gradle.properties`).

The WebGPU bindings, [webgpu-native-ffm](https://github.com/nrc4867/webgpu-native-ffm), come from GitHub Packages, which needs a login even for public packages. Put a classic personal access token with the `read:packages` scope in `~/.gradle/gradle.properties`:

```properties
GitHubPackagesUsername=<your GitHub user>
GitHubPackagesPassword=<token>
```

Then:

```
./gradlew check
```

Builds resolve wgpu-native only for the machine they run on. Pass `-Pwebgpu.natives=<platform>` (`windows-x64`, `linux-x64`, `linux-arm64`, `macos-x64` or `macos-arm64`) to package for another.

## License

GNU Affero General Public License, version 3 only. See [LICENSE](LICENSE) and [NOTICE](NOTICE).
