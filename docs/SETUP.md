# Offline app and ORBIT update — 2026-11-23

The app now runs local rules, URL inspection, bundled OCR and QR decoding. Large caches,
the Gradle distribution, build outputs and the existing test AVD have moved to an
ORBIT-backed APFS sparse bundle. This supersedes the older cache-location caveat below.
Use `zsh scripts/android.sh` and see [the current implementation/storage guide](OFFLINE_IMPLEMENTATION.md).
The earlier dated foundation evidence is retained below.

# Frontend update — 2026-11-21

The foundation has been expanded into a connected demo frontend. Current run commands,
integration boundaries and verification limits are in [frontend/README.md](frontend/README.md).
The original foundation setup and its dated validation evidence are retained below.

# Android foundation setup

This milestone is one native Android app module (`:app`) with application ID
`dev.kyzenn.nivoryn`. It displays the foundation status in Compose. The remaining
Kotlin packages and asset containers are scaffolding. Llama 3.2 1B Instruct INT4
through ExecuTorch is a future milestone; no model runtime, download or simulated
analysis is included.

## Pinned toolchain

| Component | Version |
| --- | --- |
| JDK used for verification | Eclipse Temurin 17.0.17+10, macOS AArch64 |
| Java/Kotlin JVM target | 17 |
| Android Gradle Plugin | 8.13.2 |
| Gradle wrapper | 8.13, binary distribution with SHA-256 verification |
| Kotlin Android and Compose compiler plugins | 2.2.21 |
| Compose BOM | 2025.12.01 |
| Activity Compose | 1.12.2 |
| Compile / target SDK | 36 / 36 (Android 16) |
| Minimum device SDK | 26 (Android 8.0) |
| SDK Build Tools | 35.0.0 |
| Command-line tools used for setup | 19.0 (`commandlinetools-mac-13114758_latest.zip`) |
| Platform tools used for verification | 37.0.1 (`adb` 1.0.41) |

The BOM pins Compose library versions; the compiler plugin follows Kotlin's
version instead. `./gradlew --version` reports Gradle's embedded Kotlin 2.0.21,
which is separate from the app's Kotlin plugin. No dynamic dependency versions
are used. The app deliberately uses API 26 as a conservative foundation baseline;
future model/device requirements have not been verified.

Compatibility references:

- [AGP 8.13 compatibility: Gradle 8.13, JDK 17, Build Tools 35.0.0](https://developer.android.com/build/releases/agp-8-13-0-release-notes)
- [Compose compiler plugin setup](https://developer.android.com/develop/ui/compose/compiler)
- [Compose BOM version mapping](https://developer.android.com/develop/ui/compose/bom/bom-mapping)
- [Gradle wrapper and checksum verification](https://docs.gradle.org/8.13/userguide/gradle_wrapper.html)

## Prerequisites and storage

Use a JDK 17 installation, the checked-in wrapper, an Android SDK containing only
the required platform/build tools, and network access to Google's Maven
repository, Maven Central, the Gradle plugin portal and Gradle distribution
hosts. Android Studio is optional for this command-line build; if used, select
the wrapper and JDK 17 for Gradle. A global Gradle installation is unnecessary.
An authorized Android 8.0+ phone with USB debugging is needed for runtime checks.

The repository remains at `/Users/kyzenn/Projects/nivoryn`. On this Mac:

| Resource | Location |
| --- | --- |
| New Android SDK | `/Volumes/ORBIT/Nivoryn/android-sdk` |
| New JDK | `/Volumes/ORBIT/Nivoryn/tooling/jdk/jdk-17.0.17+10/Contents/Home` |
| Downloaded tool archives / bootstrap Gradle | `/Volumes/ORBIT/Nivoryn/tooling/` |
| Working Gradle cache | `~/.gradle` on local storage; see caveat below |
| Models, datasets and other large product resources | Existing directories under `/Volumes/ORBIT/Nivoryn` |

No existing SDK or cache was migrated, and ORBIT was not reformatted. No model,
dataset, NDK, emulator, emulator image or unused SDK platform was downloaded.
The SDK contains platform 36 revision 2, Build Tools 35.0.0 and platform tools
37.0.1. The standalone command-line tools are in `cmdline-tools/19.0`.

For this machine, export these values in the shell before the commands below
(these are not repository configuration):

```sh
cd /Users/kyzenn/Projects/nivoryn
export JAVA_HOME='/Volumes/ORBIT/Nivoryn/tooling/jdk/jdk-17.0.17+10/Contents/Home'
export ANDROID_HOME='/Volumes/ORBIT/Nivoryn/android-sdk'
export GRADLE_USER_HOME="$HOME/.gradle"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/19.0/bin:$PATH"
```

Keep the SDK path in **gitignored** `local.properties`:

```properties
sdk.dir=/Volumes/ORBIT/Nivoryn/android-sdk
```

On another machine, use that machine's JDK/SDK locations. SDK components can be
installed with the following commands; review/accept the SDK licenses as needed:

```sh
sdkmanager --sdk_root="$ANDROID_HOME" --licenses
sdkmanager --sdk_root="$ANDROID_HOME" 'platform-tools' 'platforms;android-36' 'build-tools;35.0.0'
sdkmanager --sdk_root="$ANDROID_HOME" --list_installed
```

### ORBIT filesystem caveat

The initial Gradle cache on ORBIT failed while instrumenting Kotlin DSL scripts:
macOS AppleDouble sidecars such as `._Program.class` were read as JVM class files,
causing `ClassReader` errors. The build therefore uses the normal local
`~/.gradle` cache. The failed new ORBIT cache remains at
`/Volumes/ORBIT/Nivoryn/gradle-cache`; it was not migrated or reused. Keeping the
working cache on ORBIT remains blocked by this filesystem behavior.

On the same filesystem, `sdkmanager` left an extra archive-root directory inside
each newly installed package. Only these new packages were repaired in place:
`build-tools/35.0.0/android-15/*`, `platforms/android-36/android-36/*` and
`platform-tools/platform-tools/*` were moved one level up. The SDK package listing
and `adb version` then succeeded. After a future SDK install on this drive, check
that `platform-tools/adb`, `platforms/android-36/android.jar` and
`build-tools/35.0.0/aapt2` exist at the expected paths.

### Wrapper provenance and network caveat

The wrapper JAR and both scripts were generated by the official Gradle 8.13
`wrapper` task, not handwritten. The distribution SHA-256 is:

```text
20f1b1176237254a6fc204d8434196fa11a4cfb387567519c61556e8710aed78
```

The generated wrapper JAR SHA-256 is:

```text
81a82aaea5abcc8ff68b3dfcb58b3c3c429378efd98e7433460610fecd7ae45f
```

Wrapper generation against the default and direct download hosts failed its
Java HEAD request with a connection timeout. The successful generation command,
using the downloaded official Gradle distribution, was:

```sh
/Volumes/ORBIT/Nivoryn/tooling/gradle-8.13/bin/gradle wrapper \
  --gradle-version 8.13 --distribution-type bin \
  --gradle-distribution-sha256-sum 20f1b1176237254a6fc204d8434196fa11a4cfb387567519c61556e8710aed78 \
  --network-timeout 60000 --no-validate-url --console plain
```

`--no-validate-url` skips that generation-time HEAD preflight; it does not disable
the wrapper's distribution checksum check. The wrapper's Java download also
timed out following the distribution redirect on this network, although Python
download and `curl -I -L` succeeded. The already checksum-verified official ZIP
was placed in the wrapper's cache, and the genuine wrapper checked/unpacked it:

```sh
cp /Volumes/ORBIT/Nivoryn/tooling/downloads/gradle-8.13-bin.zip \
  "$GRADLE_USER_HOME/wrapper/dists/gradle-8.13-bin/5xuhj0ry160q40clulazy9h7d/gradle-8.13-bin.zip"
./gradlew --version
```

That cache directory was created by the first wrapper attempt and is specific
to the checked-in distribution URL. A normal network should allow the wrapper
to download without this workaround. Mount ORBIT before building on this Mac.

## Build and device commands

```sh
./gradlew --version
./gradlew :app:assembleDebug :app:lintDebug --console plain
adb version
adb devices -l
```

The debug APK is `app/build/outputs/apk/debug/app-debug.apk`. Lint reports are
`app/build/reports/lint-results-debug.html` and `lint-results-debug.xml`.
Generated outputs, `local.properties`, signing keys and credentials must remain
outside Git. Debug signing uses the local Android debug keystore, not a release
key checked into the repository.

When an authorized phone appears as `device` (not `unauthorized` or `offline`),
use its serial for the following **device procedure, not yet verified here**:

```sh
adb -s SERIAL install -r app/build/outputs/apk/debug/app-debug.apk
adb -s SERIAL shell am force-stop dev.kyzenn.nivoryn
adb -s SERIAL shell am start -W -n dev.kyzenn.nivoryn/.MainActivity
adb -s SERIAL shell pidof dev.kyzenn.nivoryn
adb -s SERIAL logcat -d -b crash
```

Confirm the activity remains running, inspect crash records for this package,
and visually check the title, tagline, both status lines, light/dark modes,
system-bar contrast, landscape and large font sizes. `am start` success alone
does not prove the screen stayed open. Do not clear a user's whole log buffer.

## Actual validation results — 2026-11-13

Validation ran on this Apple Silicon Mac with the environment above. The working
tree was clean before implementation and no applicable `AGENTS.md` was present.
Existing README graphics and unrelated Kotlin/asset scaffolding were preserved.

| Command / check | Actual result |
| --- | --- |
| `java -version` | Temurin 17.0.17+10 |
| `sdkmanager --sdk_root="$ANDROID_HOME" --list_installed` | Platform 36 rev 2, Build Tools 35.0.0, platform tools 37.0.1 |
| Official Gradle 8.13 `wrapper` task with arguments above | Passed; generated real JAR and scripts |
| First `./gradlew --version` | Java download timed out; resolved using the verified ZIP as described above |
| Subsequent `./gradlew --version` | Passed; Gradle 8.13, launcher/daemon JDK 17.0.17 |
| First `./gradlew :app:assembleDebug :app:lintDebug --console plain` | APK built; lint failed with 2 API compatibility errors and 4 warnings |
| Same build/lint command after fixing themes | **BUILD SUCCESSFUL**, 0 lint errors, 4 warnings |
| `"$ANDROID_HOME/build-tools/35.0.0/apksigner" verify --verbose app/build/outputs/apk/debug/app-debug.apk` | Passed; APK Signature Scheme v2 verified, one signer |
| `"$ANDROID_HOME/build-tools/35.0.0/aapt" dump badging app/build/outputs/apk/debug/app-debug.apk` | Correct application ID, version 0.1.0 (1), min SDK 26, target SDK 36 and `MainActivity` launcher |
| `"$ANDROID_HOME/build-tools/35.0.0/aapt" dump permissions app/build/outputs/apk/debug/app-debug.apk` | Only AndroidX's signature-level dynamic-receiver permission; no Internet, camera or notification permissions |
| `adb version` | 1.0.41 / platform tools 37.0.1 |
| Final `adb devices -l` outside the restricted sandbox | Exit 0; empty device list |
| `git diff --check` | Passed |

The theme fix removed the API 27-only `windowLightNavigationBar` XML attributes.
AndroidX `enableEdgeToEdge()` handles live system-bar icon contrast, while
`safeDrawingPadding()` keeps content away from bars and cutouts. The launch
window uses an indigo navigation bar with light icons on API 26. Compose follows
system light/dark mode; text wraps and the content scrolls for limited space.
These visual states have not been checked on a phone.

The four remaining lint warnings are three upgrade suggestions (Gradle, Compose
BOM and Activity Compose) and `DataExtractionRules` for `allowBackup="false"`.
The compatible versions are deliberately pinned. There is no stored user data
in this milestone; explicit device-transfer/backup rules should be revisited
when storage is introduced. No lint baseline or suppressions were added. The
initial build also reported that Compose's `libandroidx.graphics.path.so` could
not be stripped and packaged it unchanged; no NDK was downloaded for this.

Final APK: `/Users/kyzenn/Projects/nivoryn/app/build/outputs/apk/debug/app-debug.apk`
(11,373,573 bytes). SHA-256:

```text
b445f1b9c55f27b4cf1b50404c3932b07d5a59999200e3aca450d7244f83d7ab
```

**Build verified; physical-phone installation and launch not tested.** No device
was connected, so install, launch, startup-crash, visual and accessibility checks
on hardware remain pending. No emulator was installed as a substitute. No tests
that only assert placeholder text were added. The remaining setup constraints
are mounting ORBIT for this machine's SDK/JDK, keeping the Gradle cache local due
to sidecars, the documented first-download network workaround, and connecting
an authorized phone for runtime verification. No changes were committed or pushed.
