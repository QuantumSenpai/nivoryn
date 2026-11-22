#!/bin/zsh
set -euo pipefail
sdk_dir="${ANDROID_HOME:-/Volumes/ORBIT/Nivoryn/android-sdk}"
cache_image=/Volumes/ORBIT/Nivoryn/tooling/NivorynBuildCache.sparsebundle
if [[ -d "$cache_image" && ! -d /Volumes/NivorynBuildCache/android-test.avd ]]; then
  hdiutil attach "$cache_image" -mountpoint /Volumes/NivorynBuildCache -nobrowse
fi
emulator_bin="$sdk_dir/emulator/emulator"
[[ -f "$emulator_bin" ]] || emulator_bin="$emulator_bin/emulator"
system_dir="$sdk_dir/system-images/android-34/google_apis/arm64-v8a"
[[ -f "$system_dir/system.img" ]] || system_dir="$system_dir/arm64-v8a"
if [[ ! -x "$emulator_bin" || ! -f "$system_dir/system.img" ]]; then
  print -u2 'The existing Android 14 ARM64 emulator/image is unavailable. Mount ORBIT or set ANDROID_HOME.'
  exit 1
fi
export ANDROID_HOME="$sdk_dir"
export ANDROID_SDK_ROOT="$sdk_dir"
# Read-only session preserves the previous AVD; installs are disposable.
exec "$emulator_bin" -avd test -sysdir "$system_dir" -no-snapshot -read-only -gpu swiftshader -memory 2048 -cores 2 "$@"
