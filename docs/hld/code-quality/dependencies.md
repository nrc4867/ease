# Dependencies and their licenses

Everything that ships must use a permissive license. "Ships" means the runtime classpath of every module except tests, `architecture-tests` and build tooling (`build-logic`, Gradle plugins, linters). The JDK is the one exception.

## Allowed and refused

- **Allowed:** MIT, MIT-0, BSD-2-Clause, BSD-3-Clause, Apache-2.0, Zlib, ISC, UPL-1.0, Unlicense, public domain, FTL.
- **Refused:** GPL, LGPL, AGPL, MPL, EPL, CC-BY-NC and any other non-commercial terms, proprietary licenses, and anything not on the allowed list.

A library offered under a choice of licenses ships when one of the choices is allowed. Several licenses listed in one POM are read as such a choice.

## How it is checked

The `ease.licenses` convention plugin, applied to the root project, resolves every shipped module's runtime classpath together and reads each external module's license from its POM, or from the nearest parent POM that names one.

- `checkLicenses` fails when a shipped module's license is refused, is missing, or is a name it does not recognize. Names map to SPDX identifiers in `LicenseNames` in `build-logic`.
- `config/licenses/overrides.txt` sets a module's license by hand, as `group:name | license | reason`, for a POM that names none or names one unclearly. An override for a module that does not ship fails the check.
- `generateNotice` writes `NOTICE`: Ease's own notice from `config/licenses/notice-header.txt`, then each shipped dependency with its version and license. `checkNotice` fails when `NOTICE` differs from what `generateNotice` would write, so a dependency change must commit a new `NOTICE`.
- Both checks run in `./gradlew check`, and so in CI's check job.
- `LicenseCheckBuildTest` in `build-logic` builds a throwaway module that depends on an LGPL library and expects `checkLicenses` to fail, and builds one with a stale `NOTICE` and expects `checkNotice` to fail.

## Shipped dependencies

`NOTICE` is the generated list; this table adds where each one is used.

| Dependency | License | Used for |
|---|---|---|
| `dev.chieppa:webgpu-native-ffm` | MIT | wgpu bindings over FFM. Its natives jars bundle wgpu-native, offered as MIT or Apache-2.0 |
| `org.jetbrains.kotlin:kotlin-stdlib` | Apache-2.0 | The Kotlin standard library |
| `org.lwjgl:lwjgl`, `org.lwjgl:lwjgl-sdl` | BSD-3-Clause | SDL3 windows and native handles in `ease-platform`. Their natives jars bundle SDL3 (Zlib) |
| `org.jetbrains:annotations` | Apache-2.0 | Nullability annotations, pulled in by the Kotlin standard library |

Audio output is miniaudio (public domain or MIT-0) through Ease's own FFM bindings, not OpenAL Soft. Windows and input are SDL3 through LWJGL.
