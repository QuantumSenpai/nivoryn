# 0.1.3 recovery and regression report

Completed on 2026-11-24. Scope confirmed by the owner: finish and verify the interrupted 0.1.3 update. The broader animation, camera and live-monitoring roadmap remains future work.

The starting revision was `cb1c744`, the published 0.1.2 release. Uncommitted 0.1.3 work registered nonexistent network service/receiver classes, linked Settings to an unhandled route, requested unused camera/background privileges and added six unused direct dependencies. Its `singleTop` activity had no incoming-intent handler, so another share could be ignored. The website mixed 0.1.2 and 0.1.3 labels.

Removed those unfinished registrations, privileges, dependencies and navigation. Retained the version bump and keyboard resize setting. Existing offline checks, explicit model import and PCAP replay remain the supported workflows. No code comments were added.

The release helpers now use the current release metadata. Website and submission packaging accept the Android build directory already used by the build script, preventing selection of an unrelated local output. Submission packaging rejects missing or checksum-mismatched preview APKs. The source archive includes the release metadata needed by its verification scripts. Runtime smoke testing checks the installed APK checksum before exercising the UI.

The old model-staging helper reproducibly truncated the 2,183,982-byte tokenizer to 2,162,688 bytes through `adb exec-in`. Switching to the acknowledged, non-PTY shell protocol transferred both pinned resources completely; emulator SHA-256 checks passed. No model weights were added to Git or the APK.

The final signed, minified Arm64 preview is **25,552,782 bytes**, 51,083 bytes smaller than the interrupted 0.1.3 candidate. VersionCode is 4. SHA-256:

`dd85cec85b80cba852353fe8e31431ea3b3d23f49db980288af6d2734980d11f`

Verification completed:

- Three new manifest tests first failed on the interrupted implementation, then passed after repair. They cover component classes, unnecessary privileges and share-compatible launch configuration. A fourth Android host test checks Settings engine navigation and return paths.
- All **90 Android host tests** passed, with zero failures, errors or skips. Debug, preview, unsigned release and instrumentation APK builds passed. See `host-summary.json`.
- All **18 Android 14 instrumentation tests** passed with real model resources required, including offline Llama, cancellation/overlap, OCR/QR, PCAP replay, static APK inspection and encrypted history. See `android14-instrumentation.txt`.
- The exact distribution APK passed custom text → actual Llama explanation → evidence → encrypted save/reopen/delete in airplane mode on Android 14/API 34 and Android 17/API 37. The latter uses 16,384-byte pages. Both records identify the installed checksum: `android14-minified-ui.json` and `android17-minified-ui.json`. Android 17 upgraded its existing install and retained usable imported resources.
- Static distribution checks passed: preview certificate continuity, non-debuggable/non-test-only package, Arm64-only ABI, ZIP/ELF 16 KB alignment, offline permission boundary and retained ExecuTorch JNI classes. See `distribution-apk.json`.
- The visible-version regression first failed on the stale FAQ, then passed. All **17 website tests** and the static website build passed. Both local and published browser suites passed at 320, 390, 768, 1024 and 1440 pixels, with zero axe violations, plus keyboard, touch, reverse scrolling, reduced motion and no-JavaScript checks.
- The anonymous production browser download matched the exact APK size and SHA-256, correct suggested filename and Android-package headers. Production SEO checks passed for five canonical pages, social/favicon assets, sitemap/robots and the index redirect. See `publication.json` and `live-browser-report.json`.
- Both Linux observer classification tests passed. The retained packaging regression test covers missing/wrong preview inputs and detected an inverted checksum gate; a real packaging run passed APK hash, ZIP integrity, release-metadata and private-resource/key exclusion checks. That packaging check reused the historical demo video; it is not new 0.1.3 video evidence.
- A deliberately wrong release checksum was rejected by the installed-APK smoke test before any UI actions.

Lint reports zero errors and six warnings: three tool/dependency update notices, ChromeOS ABI support, conservative storage-space checking and the base launcher icon's monochrome recommendation. The separately tested runtime versions were retained. No lint suppressions or skipped tests were introduced.

Published at [nivoryn.vercel.app](https://nivoryn.vercel.app). The verified package is on the build SSD at `/Volumes/NivorynBuildCache/nivoryn-vercel-0.1.3-verified`. The preceding 0.1.2 package and deployment remain available for website rollback; an installed Android 0.1.3 should receive a higher-versionCode repair if needed.

Physical phones, Oppo-specific installation, battery/performance measurements and every Android API level were not retested. Detection effectiveness and generated advice remain experimental. These emulator regressions establish the tested workflows, not universal device compatibility or detection accuracy.
