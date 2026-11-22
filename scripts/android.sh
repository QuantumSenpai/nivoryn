#!/bin/zsh
set -euo pipefail
cd -- "$(dirname -- "$0")/.."

# This Mac keeps large build files in APFS on ORBIT (not directly on exFAT).
cache_image=/Volumes/ORBIT/Nivoryn/tooling/NivorynBuildCache.sparsebundle
if [[ -d "$cache_image" && ! -d /Volumes/NivorynBuildCache/gradle-caches ]]; then
  hdiutil attach "$cache_image" -mountpoint /Volumes/NivorynBuildCache -nobrowse
fi
if [[ -d /Volumes/NivorynBuildCache/gradle-caches ]]; then
  export NIVORYN_ANDROID_BUILD_DIR=/Volumes/NivorynBuildCache/app-build
fi
if [[ -z "${JAVA_HOME:-}" && -d /Volumes/ORBIT/Nivoryn/tooling/jdk/jdk-17.0.17+10/Contents/Home ]]; then
  export JAVA_HOME=/Volumes/ORBIT/Nivoryn/tooling/jdk/jdk-17.0.17+10/Contents/Home
fi
if (( $# == 0 )); then
  set -- :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
fi
exec ./gradlew "$@" --console plain
