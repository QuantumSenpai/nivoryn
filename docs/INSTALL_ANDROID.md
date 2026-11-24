# Install Nivoryn Guard 0.2.0

Download **Nivoryn-Guard-v0.2.0.apk** from https://nivoryn.vercel.app. This is the standalone **Arm64** early-access build (Android 8.0+), not an unsigned APK or the developer debug variant. App name/package stays Nivoryn / `dev.kyzenn.nivoryn` so existing installs can update.

Open the newly downloaded 0.2.0 file from the browser or Files. If Android asks, grant installation access to that specific source through its normal settings. Security scanning does not need to be disabled. The file is 25,572,542 bytes; its full SHA-256 is published beside the download. A renamed HTML page, incomplete transfer or old cached APK is not a valid installer.

The build uses versionCode 5 and the existing preview signing certificate, preserving update identity, history and imported models. It is release-mode/minified/non-debuggable, **still an early-access preview**; the existing certificate is not presented as an owner-managed production signing key or Google approval.

The owner reported a direct install failure on Oppo Find X9 Pro. That specific OEM failure was **not reproduced** on the stock Android 17 emulator: the previous 0.1.1 APK also installed there. Do not treat the new build as proof that every Oppo installation issue is solved. If the phone still rejects this exact new file, retain the exact installer error. On a user-authorised USB-debugging connection, `adb install -r Nivoryn-Guard-v0.2.0.apk` can expose the PackageManager reason code; a generic "App not installed" cannot identify it.

| Actual installer reason | Next step |
|---|---|
| Update/signature conflict | Confirm it is the same Nivoryn package and publisher; preserve/export existing history before considering removal. The shipped preview certificate was deliberately retained. |
| Incomplete/invalid file | Download the newly named file again and compare its size/checksum. |
| Insufficient storage | Free internal installation space; model import separately needs its documented staging/source budget. |
| Unsupported ABI | This download requires Arm64; it is not a 32-bit or x86 APK. |
| Source/policy/developer-verification restriction | Use the platform's specific installation guidance; an APK rename cannot change a device or organisation policy. |

Text/link checks and bundled OCR/QR do not require Llama installation. For actual generated explanations, import the documented pinned resources using Settings → Llama resources; see [MODEL_SETUP.md](MODEL_SETUP.md). Network support is PCAP replay and APK support is static inspection, with their displayed limits. Audio, live VPN and general cross-app malware behaviour remain unavailable.

Build: `zsh scripts/android.sh :app:assemblePreview`. Before website publishing, `scripts/verify-distribution-apk.py` rejects debug/test-only packages, wrong identity/version/certificate, missing JNI classes and native alignment failures. Current target/runtime evidence is under `docs/reports/native-0.2.0/`; the earlier installation investigation remains under `docs/reports/android-0.1.2/`. Emulator results do not establish Oppo-specific behaviour or Play Protect reputation.
